package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.QRCodeRequestIn;
import com.example.fproject.DTO.OUT.QRCodeResponseOut;
import com.example.fproject.Enum.CampaignStatus;
import com.example.fproject.Enum.QRCodeStatus;
import com.example.fproject.Model.Campaign;
import com.example.fproject.Model.QRCode;
import com.example.fproject.Repository.CampaignRepository;
import com.example.fproject.Repository.QRCodeRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QRCodeService {

    private final QRCodeRepository qrCodeRepository;
    private final CampaignRepository campaignRepository;
    private final ModelMapper modelMapper;

    public List<QRCodeResponseOut> getAllQRCodes() {
        List<QRCodeResponseOut> qrCodes = new ArrayList<>();
        for (QRCode qrCode : qrCodeRepository.findAll()) {
            qrCodes.add(mapQRCode(qrCode));
        }
        return qrCodes;
    }

    public QRCodeResponseOut getQRCodeById(Integer qrCodeId) {
        return mapQRCode(checkQRCode(qrCodeId));
    }

    @Transactional
    public QRCodeResponseOut generateQRCode(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        if (qrCodeRepository.existsByCampaignId(campaignId)) {
            throw new ApiException("Campaign already has a QR code");
        }
        if (campaign.getStatus() == CampaignStatus.EXPIRED || campaign.getStatus() == CampaignStatus.COMPLETED
                || campaign.getStatus() == CampaignStatus.CANCELED || campaign.getStatus() == CampaignStatus.STOPPED) {
            throw new ApiException("Cannot generate QR code for ended campaign");
        }
        QRCode qrCode = new QRCode();
        String code = generateUniqueCode();
        qrCode.setCode(code);
        qrCode.setQrImageBase64(generateQRCodeImage(code));
        qrCode.setMaxUsageCount(campaign.getTargetCustomersCount());
        qrCode.setUsedCount(0);
        qrCode.setStatus(QRCodeStatus.ACTIVE);
        qrCode.setCampaign(campaign);
        return mapQRCode(qrCodeRepository.save(qrCode));
    }

    public QRCodeResponseOut getQRCodeByCode(String code) {
        validateText(code, "QR code is required");
        QRCode qrCode = qrCodeRepository.findQRCodeByCode(code);
        if (qrCode == null) {
            throw new ApiException("QR code not found");
        }
        return mapQRCode(qrCode);
    }

    public byte[] getQRCodeImage(Integer qrCodeId) {
        QRCode qrCode = checkQRCode(qrCodeId);
        return Base64.getDecoder().decode(qrCode.getQrImageBase64());
    }

    public void addQRCode(QRCodeRequestIn dto) {
        validateQRCode(dto);
        validateNewQRCode(dto);
        QRCode qrCode = new QRCode();
        setQRCode(qrCode, dto);
        qrCodeRepository.save(qrCode);
    }

    public void updateQRCode(Integer qrCodeId, QRCodeRequestIn dto) {
        validateQRCode(dto);
        QRCode old = checkQRCode(qrCodeId);
        setQRCode(old, dto);
        qrCodeRepository.save(old);
    }

    public void deleteQRCode(Integer qrCodeId) {
        // Business note: this CRUD method exists for full coverage, but real workflow may update status instead of hard delete.
        qrCodeRepository.delete(checkQRCode(qrCodeId));
    }

    private void setQRCode(QRCode qrCode, QRCodeRequestIn dto) {
        qrCode.setCode(dto.getCode());
        qrCode.setQrImageBase64(generateQRCodeImage(dto.getCode()));
        qrCode.setMaxUsageCount(dto.getMaxUsageCount());
        qrCode.setUsedCount(dto.getUsedCount());
        qrCode.setStatus(dto.getStatus());
        qrCode.setCampaign(checkCampaign(dto.getCampaignId()));
    }

    private void validateQRCode(QRCodeRequestIn dto) {
        validateText(dto.getCode(), "QR code is required");
        if (dto.getUsedCount() > dto.getMaxUsageCount()) {
            throw new ApiException("Used count cannot be greater than max usage count");
        }
        if (dto.getMaxUsageCount() <= 0) {
            throw new ApiException("Max usage count must be greater than zero");
        }
        if (dto.getUsedCount() < 0) {
            throw new ApiException("Used count cannot be negative");
        }
    }

    private void validateNewQRCode(QRCodeRequestIn dto) {
        if (qrCodeRepository.existsByCampaignId(dto.getCampaignId())) {
            throw new ApiException("Campaign already has a QR code");
        }
        if (qrCodeRepository.existsByCode(dto.getCode())) {
            throw new ApiException("QR code already exists");
        }
    }

    private QRCode checkQRCode(Integer qrCodeId) {
        return qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new ApiException("QR code not found"));
    }

    private Campaign checkCampaign(Integer campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException("Campaign not found"));
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        } while (qrCodeRepository.existsByCode(code));
        return code;
    }

    private String generateQRCodeImage(String code) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(code, BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new ApiException("Failed to generate QR code image");
        }
    }

    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(message);
        }
    }

    private QRCodeResponseOut mapQRCode(QRCode qrCode) {
        QRCodeResponseOut out = modelMapper.map(qrCode, QRCodeResponseOut.class);
        out.setCampaignId(qrCode.getCampaign() == null ? null : qrCode.getCampaign().getId());
        return out;
    }
}
