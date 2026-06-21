package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.QRRedemptionCodeIn;
import com.example.fproject.DTO.IN.QRRedemptionRequestIn;
import com.example.fproject.DTO.OUT.QRRedemptionResponseOut;
import com.example.fproject.Enum.CampaignStatus;
import com.example.fproject.Enum.QRCodeStatus;
import com.example.fproject.Enum.QRRedemptionStatus;
import com.example.fproject.Model.*;
import com.example.fproject.Repository.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QRRedemptionService {

    private final QRRedemptionRepository qrRedemptionRepository;
    private final QRCodeRepository qrCodeRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignMessageRepository campaignMessageRepository;
    private final CustomerRepository customerRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final CampaignResultService campaignResultService;
    private final ModelMapper modelMapper;

    public List<QRRedemptionResponseOut> getAllQRRedemptions() {
        List<QRRedemptionResponseOut> result = new ArrayList<>();
        for (QRRedemption r : qrRedemptionRepository.findAll()) result.add(mapQRRedemption(r));
        return result;
    }

    public QRRedemptionResponseOut getQRRedemptionById(Integer userId, Integer qrRedemptionId) {
        QRRedemption r = checkQRRedemption(qrRedemptionId);
        verifyOwnership(userId, r.getCampaign());
        return mapQRRedemption(r);
    }

    public List<QRRedemptionResponseOut> getRedemptionsByCampaign(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        List<QRRedemptionResponseOut> result = new ArrayList<>();
        for (QRRedemption r : qrRedemptionRepository.findAllByCampaignId(campaignId)) result.add(mapQRRedemption(r));
        return result;
    }

    // CUSTOMER — بدون تحقق ملكية STORE_OWNER
    @Transactional
    public QRRedemptionResponseOut redeemByCode(String code, String customerPhone) {
        validateText(code, "QR code is required");
        QRCode qrCode = qrCodeRepository.findQRCodeByCode(code);
        if (qrCode == null) throw new ApiException("QR code not found");
        return redeemQRCode(qrCode, checkCustomerByPhone(customerPhone));
    }

    // CUSTOMER
    @Transactional
    public QRRedemptionResponseOut redeemByQRCodeId(Integer qrCodeId, String customerPhone) {
        return redeemQRCode(checkQRCode(qrCodeId), checkCustomerByPhone(customerPhone));
    }

    // STORE_OWNER — الكاشير
    @Transactional
    public QRRedemptionResponseOut cashierRedeemByCode(Integer userId, QRRedemptionCodeIn dto) {
        QRCode qrCode = qrCodeRepository.findQRCodeByCode(dto.getCode());
        if (qrCode == null) throw new ApiException("QR code not found");
        verifyOwnership(userId, qrCode.getCampaign());
        return redeemQRCode(qrCode, checkCustomerByPhone(dto.getCustomerPhone()));
    }

    // STORE_OWNER — الكاشير يتحقق من كود
    public String checkQRCodeForCashier(Integer userId, String code) {
        validateText(code, "QR code is required");
        QRCode qrCode = qrCodeRepository.findQRCodeByCode(code);
        if (qrCode == null) throw new ApiException("QR code not found");
        verifyOwnership(userId, qrCode.getCampaign());
        validateRedeemableQRCode(qrCode, qrCode.getCampaign());
        return "QR code is valid";
    }

    @Transactional
    public void addQRRedemption(Integer userId, QRRedemptionRequestIn dto) {
        Campaign campaign = checkCampaign(dto.getCampaignId());
        verifyOwnership(userId, campaign);
        validateQRRedemption(dto);
        QRRedemption r = new QRRedemption();
        setQRRedemption(r, dto);
        qrRedemptionRepository.save(r);
        updateRedemptionCounters(r);
    }

    @Transactional
    public void updateQRRedemption(Integer userId, Integer qrRedemptionId, QRRedemptionRequestIn dto) {
        QRRedemption old = checkQRRedemption(qrRedemptionId);
        verifyOwnership(userId, old.getCampaign());
        setQRRedemption(old, dto);
        qrRedemptionRepository.save(old);
    }

    public void deleteQRRedemption(Integer userId, Integer qrRedemptionId) {
        QRRedemption r = checkQRRedemption(qrRedemptionId);
        verifyOwnership(userId, r.getCampaign());
        qrRedemptionRepository.delete(r);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void verifyOwnership(Integer userId, Campaign campaign) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null || campaign == null || campaign.getBranch() == null
                || !campaign.getBranch().getStore().getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("You do not have permission to access this resource");
    }

    private void setQRRedemption(QRRedemption r, QRRedemptionRequestIn dto) {
        r.setRedeemedAt(dto.getRedeemedAt());
        r.setStatus(dto.getStatus());
        r.setQrCode(checkQRCode(dto.getQrCodeId()));
        r.setCampaign(checkCampaign(dto.getCampaignId()));
        r.setCustomer(checkCustomer(dto.getCustomerId()));
    }

    private void validateQRRedemption(QRRedemptionRequestIn dto) {
        QRCode qrCode    = checkQRCode(dto.getQrCodeId());
        Campaign campaign = checkCampaign(dto.getCampaignId());
        Customer customer = checkCustomer(dto.getCustomerId());
        if (qrCode.getCampaign() == null || !qrCode.getCampaign().getId().equals(campaign.getId()))
            throw new ApiException("QR code does not belong to this campaign");
        validateCustomerCanRedeem(campaign, customer);
        if (qrCode.getUsedCount() >= qrCode.getMaxUsageCount())
            throw new ApiException("QR code usage limit has been reached");
    }

    private void updateRedemptionCounters(QRRedemption r) {
        QRCode qrCode    = r.getQrCode();
        Campaign campaign = r.getCampaign();
        qrCode.setUsedCount(qrCode.getUsedCount() + 1);
        campaign.setRedeemedCount(campaign.getRedeemedCount() + 1);
        qrCodeRepository.save(qrCode);
        campaignRepository.save(campaign);
    }

    private QRRedemptionResponseOut redeemQRCode(QRCode qrCode, Customer customer) {
        Campaign campaign = qrCode.getCampaign();
        validateRedeemableQRCode(qrCode, campaign);
        validateCustomerCanRedeem(campaign, customer);

        QRRedemption r = new QRRedemption();
        r.setRedeemedAt(LocalDateTime.now());
        r.setStatus(QRRedemptionStatus.SUCCESS);
        r.setQrCode(qrCode); r.setCampaign(campaign); r.setCustomer(customer);
        QRRedemption saved = qrRedemptionRepository.save(r);

        qrCode.setUsedCount(qrCode.getUsedCount() + 1);
        campaign.setRedeemedCount(campaign.getRedeemedCount() + 1);

        if (qrCode.getUsedCount() >= qrCode.getMaxUsageCount()
                || campaign.getRedeemedCount() >= campaign.getTargetCustomersCount()) {
            qrCode.setStatus(QRCodeStatus.EXPIRED);
            campaign.setStatus(CampaignStatus.EXPIRED);
        }
        qrCodeRepository.save(qrCode);
        campaignRepository.save(campaign);
        if (campaign.getStatus() == CampaignStatus.EXPIRED)
            campaignResultService.generateCampaignResult(campaign.getId());
        return mapQRRedemption(saved);
    }

    private void validateRedeemableQRCode(QRCode qrCode, Campaign campaign) {
        if (campaign == null) throw new ApiException("QR code is not linked to campaign");
        if (qrCode.getStatus() != QRCodeStatus.ACTIVE) throw new ApiException("QR code is not active");
        if (campaign.getStatus() != CampaignStatus.ACTIVE) throw new ApiException("Campaign is not active");
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(campaign.getStartDateTime()) || now.isAfter(campaign.getEndDateTime())) {
            qrCode.setStatus(QRCodeStatus.EXPIRED); campaign.setStatus(CampaignStatus.EXPIRED);
            qrCodeRepository.save(qrCode); campaignRepository.save(campaign);
            campaignResultService.generateCampaignResult(campaign.getId());
            throw new ApiException("Campaign is outside its active time");
        }
        if (qrCode.getUsedCount() >= qrCode.getMaxUsageCount()) {
            qrCode.setStatus(QRCodeStatus.EXPIRED); qrCodeRepository.save(qrCode);
            throw new ApiException("QR code usage limit has been reached");
        }
        if (campaign.getRedeemedCount() >= campaign.getTargetCustomersCount()) {
            campaign.setStatus(CampaignStatus.EXPIRED); campaignRepository.save(campaign);
            campaignResultService.generateCampaignResult(campaign.getId());
            throw new ApiException("Campaign usage limit has been reached");
        }
    }

    private void validateCustomerCanRedeem(Campaign campaign, Customer customer) {
        if (campaign == null) throw new ApiException("Campaign is required");
        if (customer == null) throw new ApiException("Customer is required");
        if (Boolean.TRUE.equals(qrRedemptionRepository.existsByCampaignIdAndCustomerId(campaign.getId(), customer.getId())))
            throw new ApiException("Customer already used this QR code for this campaign");
        if (!Boolean.TRUE.equals(campaignMessageRepository.existsByCampaignIdAndCustomerId(campaign.getId(), customer.getId())))
            throw new ApiException("Customer did not receive this campaign offer");
    }

    private QRRedemption checkQRRedemption(Integer id) {
        return qrRedemptionRepository.findById(id).orElseThrow(() -> new ApiException("QR redemption not found"));
    }

    private QRCode checkQRCode(Integer id) {
        return qrCodeRepository.findById(id).orElseThrow(() -> new ApiException("QR code not found"));
    }

    private Campaign checkCampaign(Integer id) {
        return campaignRepository.findById(id).orElseThrow(() -> new ApiException("Campaign not found"));
    }

    private Customer checkCustomer(Integer id) {
        return customerRepository.findById(id).orElseThrow(() -> new ApiException("Customer not found"));
    }

    private Customer checkCustomerByPhone(String phone) {
        validateText(phone, "Customer phone is required");
        String normalized = phone.replace("whatsapp:", "").trim();
        for (Customer c : customerRepository.findAll()) {
            if (c.getUser() != null && c.getUser().getPhone() != null
                    && c.getUser().getPhone().replace("whatsapp:", "").trim().equals(normalized)) return c;
        }
        throw new ApiException("Customer not found");
    }

    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) throw new ApiException(message);
    }

    private QRRedemptionResponseOut mapQRRedemption(QRRedemption r) {
        QRRedemptionResponseOut out = modelMapper.map(r, QRRedemptionResponseOut.class);
        out.setQrCodeId(r.getQrCode() == null ? null : r.getQrCode().getId());
        out.setCampaignId(r.getCampaign() == null ? null : r.getCampaign().getId());
        out.setCustomerId(r.getCustomer() == null ? null : r.getCustomer().getId());
        return out;
    }
}