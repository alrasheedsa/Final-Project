package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.QRCodeRequestIn;
import com.example.fproject.DTO.OUT.QRCodeResponseOut;
import com.example.fproject.Enum.CampaignStatus;
import com.example.fproject.Enum.QRCodeStatus;
import com.example.fproject.Model.Campaign;
import com.example.fproject.Model.QRCode;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Repository.CampaignRepository;
import com.example.fproject.Repository.QRCodeRepository;
import com.example.fproject.Repository.StoreOwnerRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QRCodeService {

    private final QRCodeRepository qrCodeRepository;
    private final CampaignRepository campaignRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final ModelMapper modelMapper;

    public List<QRCodeResponseOut> getAllQRCodes() {
        List<QRCodeResponseOut> result = new ArrayList<>();
        for (QRCode q : qrCodeRepository.findAll()) result.add(mapQRCode(q));
        return result;
    }

    public QRCodeResponseOut getQRCodeById(Integer userId, Integer qrCodeId) {
        QRCode q = checkQRCode(qrCodeId);
        verifyOwnership(userId, q.getCampaign());
        return mapQRCode(q);
    }

    public QRCodeResponseOut getQRCodeByCode(Integer userId, String code) {
        validateText(code, "QR code is required");
        QRCode q = qrCodeRepository.findQRCodeByCode(code);
        if (q == null) throw new ApiException("QR code not found");
        verifyOwnership(userId, q.getCampaign());
        return mapQRCode(q);
    }

    public byte[] getQRCodeImage(Integer userId, Integer qrCodeId) {
        QRCode q = checkQRCode(qrCodeId);
        verifyOwnership(userId, q.getCampaign());
        return Base64.getDecoder().decode(q.getQrImageBase64());
    }

    @Transactional
    public QRCodeResponseOut generateQRCode(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        if (qrCodeRepository.existsByCampaignId(campaignId)) throw new ApiException("Campaign already has a QR code");
        if (campaign.getStatus() == CampaignStatus.EXPIRED || campaign.getStatus() == CampaignStatus.COMPLETED
                || campaign.getStatus() == CampaignStatus.CANCELED || campaign.getStatus() == CampaignStatus.STOPPED)
            throw new ApiException("Cannot generate QR code for ended campaign");
        QRCode q = new QRCode();
        String code = generateUniqueCode();
        q.setCode(code);
        q.setQrImageBase64(generateQRCodeImage(code));
        q.setMaxUsageCount(campaign.getTargetCustomersCount());
        q.setUsedCount(0);
        q.setStatus(QRCodeStatus.ACTIVE);
        q.setCampaign(campaign);
        return mapQRCode(qrCodeRepository.save(q));
    }

    public void addQRCode(Integer userId, QRCodeRequestIn dto) {
        Campaign campaign = checkCampaign(dto.getCampaignId());
        verifyOwnership(userId, campaign);
        validateQRCode(dto);
        validateNewQRCode(dto);
        QRCode q = new QRCode();
        setQRCode(q, dto);
        qrCodeRepository.save(q);
    }

    public void updateQRCode(Integer userId, Integer qrCodeId, QRCodeRequestIn dto) {
        QRCode old = checkQRCode(qrCodeId);
        verifyOwnership(userId, old.getCampaign());
        validateQRCode(dto);
        setQRCode(old, dto);
        qrCodeRepository.save(old);
    }

    public void deleteQRCode(Integer userId, Integer qrCodeId) {
        QRCode q = checkQRCode(qrCodeId);
        verifyOwnership(userId, q.getCampaign());
        qrCodeRepository.delete(q);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void verifyOwnership(Integer userId, Campaign campaign) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null || campaign == null || campaign.getBranch() == null
                || !campaign.getBranch().getStore().getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("You do not have permission to access this resource");
    }

    private void setQRCode(QRCode q, QRCodeRequestIn dto) {
        q.setCode(dto.getCode());
        q.setQrImageBase64(generateQRCodeImage(dto.getCode()));
        q.setMaxUsageCount(dto.getMaxUsageCount());
        q.setUsedCount(dto.getUsedCount());
        q.setStatus(dto.getStatus());
        q.setCampaign(checkCampaign(dto.getCampaignId()));
    }

    private void validateQRCode(QRCodeRequestIn dto) {
        validateText(dto.getCode(), "QR code is required");
        if (dto.getUsedCount() > dto.getMaxUsageCount()) throw new ApiException("Used count cannot be greater than max usage count");
        if (dto.getMaxUsageCount() <= 0) throw new ApiException("Max usage count must be greater than zero");
        if (dto.getUsedCount() < 0) throw new ApiException("Used count cannot be negative");
    }

    private void validateNewQRCode(QRCodeRequestIn dto) {
        if (qrCodeRepository.existsByCampaignId(dto.getCampaignId())) throw new ApiException("Campaign already has a QR code");
        if (qrCodeRepository.existsByCode(dto.getCode())) throw new ApiException("QR code already exists");
    }

    private QRCode checkQRCode(Integer id) {
        return qrCodeRepository.findById(id).orElseThrow(() -> new ApiException("QR code not found"));
    }

    private Campaign checkCampaign(Integer id) {
        return campaignRepository.findById(id).orElseThrow(() -> new ApiException("Campaign not found"));
    }

    private String generateUniqueCode() {
        String code;
        do { code = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(); }
        while (qrCodeRepository.existsByCode(code));
        return code;
    }

    private String generateQRCodeImage(String code) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(code, BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) { throw new ApiException("Failed to generate QR code image"); }
    }

    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) throw new ApiException(message);
    }

    private QRCodeResponseOut mapQRCode(QRCode q) {
        QRCodeResponseOut out = modelMapper.map(q, QRCodeResponseOut.class);
        out.setCampaignId(q.getCampaign() == null ? null : q.getCampaign().getId());
        return out;
    }
}