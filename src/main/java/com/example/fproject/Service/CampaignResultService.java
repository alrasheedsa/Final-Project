package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.CampaignResultRequestIn;
import com.example.fproject.DTO.OUT.CampaignResultResponseOut;
import com.example.fproject.DTO.OUT.ValueOut;
import com.example.fproject.Model.Campaign;
import com.example.fproject.Model.CampaignMessage;
import com.example.fproject.Model.CampaignResult;
import com.example.fproject.Model.CustomerAnswer;
import com.example.fproject.Model.MonthlyReport;
import com.example.fproject.Repository.CampaignMessageRepository;
import com.example.fproject.Repository.CampaignRepository;
import com.example.fproject.Repository.CampaignResultRepository;
import com.example.fproject.Repository.CustomerAnswerRepository;
import com.example.fproject.Repository.MonthlyReportRepository;
import com.example.fproject.Repository.QRRedemptionRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignResultService {

    private final CampaignResultRepository campaignResultRepository;
    private final CampaignRepository campaignRepository;
    private final MonthlyReportRepository monthlyReportRepository;
    private final CampaignMessageRepository campaignMessageRepository;
    private final CustomerAnswerRepository customerAnswerRepository;
    private final QRRedemptionRepository qrRedemptionRepository;
    private final ModelMapper modelMapper;

    public List<CampaignResultResponseOut> getAllCampaignResults() {
        List<CampaignResultResponseOut> results = new ArrayList<>();
        for (CampaignResult result : campaignResultRepository.findAll()) {
            results.add(mapCampaignResult(result));
        }
        return results;
    }

    public CampaignResultResponseOut getCampaignResultById(Integer campaignResultId) {
        return mapCampaignResult(checkCampaignResult(campaignResultId));
    }

    public CampaignResultResponseOut generateCampaignResult(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        CampaignResult campaignResult = campaignResultRepository.findCampaignResultByCampaignId(campaignId);
        if (campaignResult == null) {
            campaignResult = new CampaignResult();
        }
        Integer totalSent = campaign.getSentCount();
        Integer qrUsed = qrRedemptionRepository.countByCampaignId(campaignId);
        Double conversionRate = calculateConversionRate(totalSent, qrUsed);
        campaignResult.setSentCount(totalSent);
        campaignResult.setRedeemedCount(qrUsed);
        campaignResult.setConversionRate(conversionRate);
        campaignResult.setAiSummary(buildCampaignSummary(campaignId, totalSent, qrUsed, conversionRate));
        campaignResult.setCreatedAt(LocalDateTime.now());
        CampaignResult saved = campaignResultRepository.save(campaignResult);
        campaign.setCampaignResult(saved);
        campaignRepository.save(campaign);
        return mapCampaignResult(saved);
    }

    public CampaignResultResponseOut getCampaignResultByCampaign(Integer campaignId) {
        checkCampaign(campaignId);
        CampaignResult campaignResult = campaignResultRepository.findCampaignResultByCampaignId(campaignId);
        if (campaignResult == null) {
            throw new ApiException("Campaign result not found");
        }
        return mapCampaignResult(campaignResult);
    }

    public ValueOut getTotalSent(Integer campaignId) {
        return new ValueOut(checkCampaign(campaignId).getSentCount());
    }

    public ValueOut getTotalAnswered(Integer campaignId) {
        checkCampaign(campaignId);
        return new ValueOut(customerAnswerRepository.countByCampaignId(campaignId));
    }

    public ValueOut getCorrectAnswers(Integer campaignId) {
        checkCampaign(campaignId);
        return new ValueOut(customerAnswerRepository.countByCampaignIdAndCorrect(campaignId, true));
    }

    public ValueOut getWrongAnswers(Integer campaignId) {
        checkCampaign(campaignId);
        return new ValueOut(customerAnswerRepository.countByCampaignIdAndCorrect(campaignId, false));
    }

    public ValueOut getQRUsed(Integer campaignId) {
        checkCampaign(campaignId);
        return new ValueOut(qrRedemptionRepository.countByCampaignId(campaignId));
    }

    public ValueOut getConversionRate(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        return new ValueOut(calculateConversionRate(campaign.getSentCount(), qrRedemptionRepository.countByCampaignId(campaignId)));
    }

    public ValueOut getBestResponseTime(Integer campaignId) {
        checkCampaign(campaignId);
        Long bestMinutes = null;
        for (CustomerAnswer answer : customerAnswerRepository.findAllByCampaignId(campaignId)) {
            CampaignMessage message = answer.getCampaignMessage();
            if (message == null || message.getSentAt() == null || answer.getAttemptedAt() == null) {
                continue;
            }
            long minutes = Duration.between(message.getSentAt(), answer.getAttemptedAt()).toMinutes();
            if (bestMinutes == null || minutes < bestMinutes) {
                bestMinutes = minutes;
            }
        }
        if (bestMinutes == null) {
            return new ValueOut("No answers yet");
        }
        return new ValueOut(bestMinutes + " minutes");
    }

    public void addCampaignResult(CampaignResultRequestIn dto) {
        validateCampaignResult(dto);
        CampaignResult campaignResult = new CampaignResult();
        setCampaignResult(campaignResult, dto);
        CampaignResult saved = campaignResultRepository.save(campaignResult);
        linkCampaign(saved, dto.getCampaignId());
    }

    public void updateCampaignResult(Integer campaignResultId, CampaignResultRequestIn dto) {
        validateCampaignResult(dto);
        CampaignResult old = checkCampaignResult(campaignResultId);
        setCampaignResult(old, dto);
        CampaignResult saved = campaignResultRepository.save(old);
        linkCampaign(saved, dto.getCampaignId());
    }

    public void deleteCampaignResult(Integer campaignResultId) {
        // Business note: this CRUD method exists for full coverage, but real workflow may keep final results for monthly reports.
        campaignResultRepository.delete(checkCampaignResult(campaignResultId));
    }

    private void setCampaignResult(CampaignResult campaignResult, CampaignResultRequestIn dto) {
        campaignResult.setSentCount(dto.getSentCount());
        campaignResult.setRedeemedCount(dto.getRedeemedCount());
        campaignResult.setConversionRate(dto.getConversionRate());
        campaignResult.setAiSummary(dto.getAiSummary());
        campaignResult.setCreatedAt(dto.getCreatedAt());
        campaignResult.setMonthlyReport(checkMonthlyReport(dto.getMonthlyReportId()));
    }

    private void validateCampaignResult(CampaignResultRequestIn dto) {
        if (dto.getRedeemedCount() > dto.getSentCount()) {
            throw new ApiException("Redeemed count cannot be greater than sent count");
        }
        if (dto.getConversionRate() > 100) {
            throw new ApiException("Conversion rate cannot be greater than 100");
        }
        if (dto.getSentCount() == 0 && dto.getConversionRate() > 0) {
            throw new ApiException("Conversion rate must be zero when sent count is zero");
        }
    }

    private void linkCampaign(CampaignResult campaignResult, Integer campaignId) {
        if (campaignId == null) return;
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException("Campaign not found"));
        if (campaign.getCampaignResult() != null && !campaign.getCampaignResult().getId().equals(campaignResult.getId())) {
            throw new ApiException("Campaign already has a campaign result");
        }
        campaign.setCampaignResult(campaignResult);
        campaignRepository.save(campaign);
    }

    private CampaignResult checkCampaignResult(Integer campaignResultId) {
        return campaignResultRepository.findById(campaignResultId)
                .orElseThrow(() -> new ApiException("Campaign result not found"));
    }

    private Campaign checkCampaign(Integer campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException("Campaign not found"));
    }

    private MonthlyReport checkMonthlyReport(Integer monthlyReportId) {
        if (monthlyReportId == null) return null;
        return monthlyReportRepository.findById(monthlyReportId)
                .orElseThrow(() -> new ApiException("Monthly report not found"));
    }

    private CampaignResultResponseOut mapCampaignResult(CampaignResult campaignResult) {
        CampaignResultResponseOut out = modelMapper.map(campaignResult, CampaignResultResponseOut.class);
        out.setCampaignId(campaignResult.getCampaign() == null ? null : campaignResult.getCampaign().getId());
        out.setMonthlyReportId(campaignResult.getMonthlyReport() == null ? null : campaignResult.getMonthlyReport().getId());
        return out;
    }

    private Double calculateConversionRate(Integer sentCount, Integer qrUsed) {
        if (sentCount == null || sentCount == 0) {
            return 0.0;
        }
        return (qrUsed * 100.0) / sentCount;
    }

    private String buildCampaignSummary(Integer campaignId, Integer totalSent, Integer qrUsed, Double conversionRate) {
        Integer totalAnswered = customerAnswerRepository.countByCampaignId(campaignId);
        Integer correctAnswers = customerAnswerRepository.countByCampaignIdAndCorrect(campaignId, true);
        Integer wrongAnswers = customerAnswerRepository.countByCampaignIdAndCorrect(campaignId, false);
        return "Sent: " + totalSent
                + ", answered: " + totalAnswered
                + ", correct answers: " + correctAnswers
                + ", wrong answers: " + wrongAnswers
                + ", QR used: " + qrUsed
                + ", conversion rate: " + conversionRate + "%";
    }
}
