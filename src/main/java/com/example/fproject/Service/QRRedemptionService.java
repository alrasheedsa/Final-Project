package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.QRRedemptionRequestIn;
import com.example.fproject.DTO.OUT.QRRedemptionResponseOut;
import com.example.fproject.Model.Campaign;
import com.example.fproject.Model.QRCode;
import com.example.fproject.Model.QRRedemption;
import com.example.fproject.Repository.CampaignRepository;
import com.example.fproject.Repository.QRCodeRepository;
import com.example.fproject.Repository.QRRedemptionRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QRRedemptionService {

    private final QRRedemptionRepository qrRedemptionRepository;
    private final QRCodeRepository qrCodeRepository;
    private final CampaignRepository campaignRepository;
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
    }

    private void validateQRRedemption(QRRedemptionRequestIn dto) {
        QRCode qrCode = checkQRCode(dto.getQrCodeId());
        Campaign campaign = checkCampaign(dto.getCampaignId());
        if (qrCode.getCampaign() == null || !qrCode.getCampaign().getId().equals(campaign.getId())) {
            throw new ApiException("QR code does not belong to this campaign");
        }
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

    private QRRedemptionResponseOut mapQRRedemption(QRRedemption qrRedemption) {
        QRRedemptionResponseOut out = modelMapper.map(qrRedemption, QRRedemptionResponseOut.class);
        out.setQrCodeId(qrRedemption.getQrCode() == null ? null : qrRedemption.getQrCode().getId());
        out.setCampaignId(qrRedemption.getCampaign() == null ? null : qrRedemption.getCampaign().getId());
        return out;
    }
}
