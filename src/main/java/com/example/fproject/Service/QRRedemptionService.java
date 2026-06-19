package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.QRRedemptionCodeIn;
import com.example.fproject.DTO.IN.QRRedemptionRequestIn;
import com.example.fproject.DTO.OUT.QRRedemptionResponseOut;
import com.example.fproject.Enum.CampaignStatus;
import com.example.fproject.Enum.QRCodeStatus;
import com.example.fproject.Enum.QRRedemptionStatus;
import com.example.fproject.Model.Campaign;
import com.example.fproject.Model.Customer;
import com.example.fproject.Model.QRCode;
import com.example.fproject.Model.QRRedemption;
import com.example.fproject.Repository.CampaignRepository;
import com.example.fproject.Repository.CustomerRepository;
import com.example.fproject.Repository.QRCodeRepository;
import com.example.fproject.Repository.QRRedemptionRepository;
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
    private final CustomerRepository customerRepository;
    private final CampaignResultService campaignResultService;
    private final ModelMapper modelMapper;

    public List<QRRedemptionResponseOut> getAllQRRedemptions() {
        List<QRRedemptionResponseOut> redemptions = new ArrayList<>();
        for (QRRedemption redemption : qrRedemptionRepository.findAll()) {
            redemptions.add(mapQRRedemption(redemption));
        }
        return redemptions;
    }

    public QRRedemptionResponseOut getQRRedemptionById(Integer qrRedemptionId) {
        return mapQRRedemption(checkQRRedemption(qrRedemptionId));
    }

    @Transactional
    public QRRedemptionResponseOut redeemByCode(String code, String customerPhone) {
        validateText(code, "QR code is required");
        QRCode qrCode = qrCodeRepository.findQRCodeByCode(code);
        if (qrCode == null) {
            throw new ApiException("QR code not found");
        }
        return redeemQRCode(qrCode, checkCustomerByPhone(customerPhone));
    }

    @Transactional
    public QRRedemptionResponseOut redeemByQRCodeId(Integer qrCodeId, String customerPhone) {
        return redeemQRCode(checkQRCode(qrCodeId), checkCustomerByPhone(customerPhone));
    }

    @Transactional
    public QRRedemptionResponseOut cashierRedeemByCode(QRRedemptionCodeIn dto) {
        return redeemByCode(dto.getCode(), dto.getCustomerPhone());
    }

    public String checkQRCodeForCashier(String code) {
        validateText(code, "QR code is required");
        QRCode qrCode = qrCodeRepository.findQRCodeByCode(code);
        if (qrCode == null) {
            throw new ApiException("QR code not found");
        }
        validateRedeemableQRCode(qrCode, qrCode.getCampaign());
        return "QR code is valid";
    }

    public List<QRRedemptionResponseOut> getRedemptionsByCampaign(Integer campaignId) {
        checkCampaign(campaignId);
        List<QRRedemptionResponseOut> redemptions = new ArrayList<>();
        for (QRRedemption redemption : qrRedemptionRepository.findAllByCampaignId(campaignId)) {
            redemptions.add(mapQRRedemption(redemption));
        }
        return redemptions;
    }

    @Transactional
    public void addQRRedemption(QRRedemptionRequestIn dto) {
        validateQRRedemption(dto);
        QRRedemption qrRedemption = new QRRedemption();
        setQRRedemption(qrRedemption, dto);
        qrRedemptionRepository.save(qrRedemption);
        updateRedemptionCounters(qrRedemption);
    }

    public void updateQRRedemption(Integer qrRedemptionId, QRRedemptionRequestIn dto) {
        QRRedemption old = checkQRRedemption(qrRedemptionId);
        setQRRedemption(old, dto);
        qrRedemptionRepository.save(old);
    }

    public void deleteQRRedemption(Integer qrRedemptionId) {
        // Business note: this CRUD method exists for full coverage, but real workflow may keep QR redemption records for reports.
        qrRedemptionRepository.delete(checkQRRedemption(qrRedemptionId));
    }

    private void setQRRedemption(QRRedemption qrRedemption, QRRedemptionRequestIn dto) {
        qrRedemption.setRedeemedAt(dto.getRedeemedAt());
        qrRedemption.setStatus(dto.getStatus());
        qrRedemption.setQrCode(checkQRCode(dto.getQrCodeId()));
        qrRedemption.setCampaign(checkCampaign(dto.getCampaignId()));
        qrRedemption.setCustomer(checkCustomer(dto.getCustomerId()));
    }

    private void validateQRRedemption(QRRedemptionRequestIn dto) {
        QRCode qrCode = checkQRCode(dto.getQrCodeId());
        Campaign campaign = checkCampaign(dto.getCampaignId());
        Customer customer = checkCustomer(dto.getCustomerId());
        if (qrCode.getCampaign() == null || !qrCode.getCampaign().getId().equals(campaign.getId())) {
            throw new ApiException("QR code does not belong to this campaign");
        }
        validateCustomerCanRedeem(campaign, customer);
        if (qrCode.getUsedCount() >= qrCode.getMaxUsageCount()) {
            throw new ApiException("QR code usage limit has been reached");
        }
    }

    private void updateRedemptionCounters(QRRedemption qrRedemption) {
        QRCode qrCode = qrRedemption.getQrCode();
        Campaign campaign = qrRedemption.getCampaign();
        qrCode.setUsedCount(qrCode.getUsedCount() + 1);
        campaign.setRedeemedCount(campaign.getRedeemedCount() + 1);
        qrCodeRepository.save(qrCode);
        campaignRepository.save(campaign);
    }

    private QRRedemptionResponseOut redeemQRCode(QRCode qrCode, Customer customer) {
        Campaign campaign = qrCode.getCampaign();
        validateRedeemableQRCode(qrCode, campaign);
        validateCustomerCanRedeem(campaign, customer);

        QRRedemption qrRedemption = new QRRedemption();
        qrRedemption.setRedeemedAt(LocalDateTime.now());
        qrRedemption.setStatus(QRRedemptionStatus.SUCCESS);
        qrRedemption.setQrCode(qrCode);
        qrRedemption.setCampaign(campaign);
        qrRedemption.setCustomer(customer);
        QRRedemption saved = qrRedemptionRepository.save(qrRedemption);

        qrCode.setUsedCount(qrCode.getUsedCount() + 1);
        campaign.setRedeemedCount(campaign.getRedeemedCount() + 1);
        if (qrCode.getUsedCount() >= qrCode.getMaxUsageCount()
                || campaign.getRedeemedCount() >= campaign.getTargetCustomersCount()) {
            qrCode.setStatus(QRCodeStatus.EXPIRED);
            campaign.setStatus(CampaignStatus.EXPIRED);
        }
        qrCodeRepository.save(qrCode);
        campaignRepository.save(campaign);
        if (campaign.getStatus() == CampaignStatus.EXPIRED) {
            campaignResultService.generateCampaignResult(campaign.getId());
        }
        return mapQRRedemption(saved);
    }

    private void validateRedeemableQRCode(QRCode qrCode, Campaign campaign) {
        if (campaign == null) {
            throw new ApiException("QR code is not linked to campaign");
        }
        if (qrCode.getStatus() != QRCodeStatus.ACTIVE) {
            throw new ApiException("QR code is not active");
        }
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new ApiException("Campaign is not active");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(campaign.getStartDateTime()) || now.isAfter(campaign.getEndDateTime())) {
            qrCode.setStatus(QRCodeStatus.EXPIRED);
            campaign.setStatus(CampaignStatus.EXPIRED);
            qrCodeRepository.save(qrCode);
            campaignRepository.save(campaign);
            campaignResultService.generateCampaignResult(campaign.getId());
            throw new ApiException("Campaign is outside its active time");
        }
        if (qrCode.getUsedCount() >= qrCode.getMaxUsageCount()) {
            qrCode.setStatus(QRCodeStatus.EXPIRED);
            qrCodeRepository.save(qrCode);
            throw new ApiException("QR code usage limit has been reached");
        }
        if (campaign.getRedeemedCount() >= campaign.getTargetCustomersCount()) {
            campaign.setStatus(CampaignStatus.EXPIRED);
            campaignRepository.save(campaign);
            campaignResultService.generateCampaignResult(campaign.getId());
            throw new ApiException("Campaign usage limit has been reached");
        }
    }

    private void validateCustomerCanRedeem(Campaign campaign, Customer customer) {
        if (campaign == null) {
            throw new ApiException("Campaign is required");
        }
        if (customer == null) {
            throw new ApiException("Customer is required");
        }
        if (Boolean.TRUE.equals(qrRedemptionRepository.existsByCampaignIdAndCustomerId(campaign.getId(), customer.getId()))) {
            throw new ApiException("Customer already used this QR code for this campaign");
        }
    }

    private QRRedemption checkQRRedemption(Integer qrRedemptionId) {
        return qrRedemptionRepository.findById(qrRedemptionId)
                .orElseThrow(() -> new ApiException("QR redemption not found"));
    }

    private QRCode checkQRCode(Integer qrCodeId) {
        return qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new ApiException("QR code not found"));
    }

    private Campaign checkCampaign(Integer campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException("Campaign not found"));
    }

    private Customer checkCustomer(Integer customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ApiException("Customer not found"));
    }

    private Customer checkCustomerByPhone(String phone) {
        validateText(phone, "Customer phone is required");
        String normalizedPhone = normalizeStoredPhone(phone);
        for (Customer customer : customerRepository.findAll()) {
            if (customer.getUser() != null
                    && customer.getUser().getPhone() != null
                    && normalizeStoredPhone(customer.getUser().getPhone()).equals(normalizedPhone)) {
                return customer;
            }
        }
        throw new ApiException("Customer not found");
    }

    private String normalizeStoredPhone(String phone) {
        String value = phone.replace("whatsapp:", "").replaceAll("[^0-9]", "");
        if (value.startsWith("966")) {
            return "0" + value.substring(3);
        }
        if (value.startsWith("5")) {
            return "0" + value;
        }
        return value;
    }

    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(message);
        }
    }

    private QRRedemptionResponseOut mapQRRedemption(QRRedemption qrRedemption) {
        QRRedemptionResponseOut out = modelMapper.map(qrRedemption, QRRedemptionResponseOut.class);
        out.setQrCodeId(qrRedemption.getQrCode() == null ? null : qrRedemption.getQrCode().getId());
        out.setCampaignId(qrRedemption.getCampaign() == null ? null : qrRedemption.getCampaign().getId());
        out.setCustomerId(qrRedemption.getCustomer() == null ? null : qrRedemption.getCustomer().getId());
        return out;
    }
}
