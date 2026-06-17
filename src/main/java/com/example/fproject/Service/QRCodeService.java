package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.QRCodeRequestIn;
import com.example.fproject.DTO.OUT.QRCodeResponseOut;
import com.example.fproject.Model.Campaign;
import com.example.fproject.Model.QRCode;
import com.example.fproject.Repository.CampaignRepository;
import com.example.fproject.Repository.QRCodeRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
        qrCode.setMaxUsageCount(dto.getMaxUsageCount());
        qrCode.setUsedCount(dto.getUsedCount());
        qrCode.setStatus(dto.getStatus());
        qrCode.setCampaign(checkCampaign(dto.getCampaignId()));
    }

    private void validateQRCode(QRCodeRequestIn dto) {
        if (dto.getUsedCount() > dto.getMaxUsageCount()) {
            throw new ApiException("Used count cannot be greater than max usage count");
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

    private QRCodeResponseOut mapQRCode(QRCode qrCode) {
        QRCodeResponseOut out = modelMapper.map(qrCode, QRCodeResponseOut.class);
        out.setCampaignId(qrCode.getCampaign() == null ? null : qrCode.getCampaign().getId());
        return out;
    }
}
