package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.CampaignResultRequestIn;
import com.example.fproject.DTO.OUT.CampaignResultResponseOut;
import com.example.fproject.DTO.OUT.ValueOut;
import com.example.fproject.Enum.CampaignStatus;
import com.example.fproject.Model.*;
import com.example.fproject.Repository.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CampaignResultService {

    private final CampaignResultRepository campaignResultRepository;
    private final CampaignRepository campaignRepository;
    private final MonthlyReportRepository monthlyReportRepository;
    private final CampaignMessageRepository campaignMessageRepository;
    private final CustomerAnswerRepository customerAnswerRepository;
    private final QRRedemptionRepository qrRedemptionRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final ModelMapper modelMapper;

    public List<CampaignResultResponseOut> getAllCampaignResults() {
        List<CampaignResultResponseOut> result = new ArrayList<>();
        for (CampaignResult r : campaignResultRepository.findAll()) result.add(mapCampaignResult(r));
        return result;
    }

    public CampaignResultResponseOut getCampaignResultById(Integer userId, Integer campaignResultId) {
        CampaignResult r = checkCampaignResult(campaignResultId);
        verifyOwnership(userId, r.getCampaign());
        return mapCampaignResult(r);
    }

    public CampaignResultResponseOut getCampaignResultByCampaign(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        CampaignResult r = campaignResultRepository.findCampaignResultByCampaignId(campaignId);
        if (r == null) throw new ApiException("Campaign result not found");
        return mapCampaignResult(r);
    }

    public ValueOut getTotalSent(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        return new ValueOut(campaign.getSentCount());
    }

    public ValueOut getTotalAnswered(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        return new ValueOut(customerAnswerRepository.countByCampaignId(campaignId));
    }

    public ValueOut getCorrectAnswers(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        return new ValueOut(customerAnswerRepository.countByCampaignIdAndCorrect(campaignId, true));
    }

    public ValueOut getWrongAnswers(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        return new ValueOut(customerAnswerRepository.countByCampaignIdAndCorrect(campaignId, false));
    }

    public ValueOut getQRUsed(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        return new ValueOut(qrRedemptionRepository.countByCampaignId(campaignId));
    }

    public ValueOut getConversionRate(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        return new ValueOut(calculateConversionRate(campaign.getSentCount(),
                qrRedemptionRepository.countByCampaignId(campaignId)));
    }

    public ValueOut getBestResponseTime(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        Long bestMinutes = null;
        for (CustomerAnswer answer : customerAnswerRepository.findAllByCampaignId(campaignId)) {
            CampaignMessage message = answer.getCampaignMessage();
            if (message == null || message.getSentAt() == null || answer.getAttemptedAt() == null) continue;
            long minutes = Duration.between(message.getSentAt(), answer.getAttemptedAt()).toMinutes();
            if (bestMinutes == null || minutes < bestMinutes) bestMinutes = minutes;
        }
        return bestMinutes == null ? new ValueOut("No answers yet") : new ValueOut(bestMinutes + " minutes");
    }

    // ADMIN — بدون تحقق ملكية
    @Transactional
    public List<CampaignResultResponseOut> generateFinishedCampaignResults() {
        List<CampaignResultResponseOut> generated = new ArrayList<>();
        for (Campaign c : campaignRepository.findAllByStatus(CampaignStatus.EXPIRED)) {
            if (campaignResultRepository.findCampaignResultByCampaignId(c.getId()) == null)
                generated.add(generateCampaignResult(c.getId()));
        }
        for (Campaign c : campaignRepository.findAllByStatus(CampaignStatus.COMPLETED)) {
            if (campaignResultRepository.findCampaignResultByCampaignId(c.getId()) == null)
                generated.add(generateCampaignResult(c.getId()));
        }
        return generated;
    }

    public Map<String, Object> getCampaignResultDashboard(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        CampaignResult r = campaignResultRepository.findCampaignResultByCampaignId(campaignId);
        if (r == null) throw new ApiException("Campaign result not found");
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("campaignId", campaign.getId());
        dashboard.put("campaignTitle", campaign.getTitle());
        dashboard.put("campaignStatus", campaign.getStatus());
        dashboard.put("result", mapCampaignResult(r));
        dashboard.put("totalSent", getTotalSent(userId, campaignId));
        dashboard.put("totalAnswered", getTotalAnswered(userId, campaignId));
        dashboard.put("correctAnswers", getCorrectAnswers(userId, campaignId));
        dashboard.put("wrongAnswers", getWrongAnswers(userId, campaignId));
        dashboard.put("qrUsed", getQRUsed(userId, campaignId));
        dashboard.put("conversionRate", getConversionRate(userId, campaignId));
        dashboard.put("bestResponseTime", getBestResponseTime(userId, campaignId));
        dashboard.put("aiSummary", r.getAiSummary());
        dashboard.put("createdAt", r.getCreatedAt());
        return dashboard;
    }

    // استخدام داخلي (من CampaignService وQRRedemptionService) — بدون userId
    @Transactional
    public CampaignResultResponseOut generateCampaignResult(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        CampaignResult r = campaignResultRepository.findCampaignResultByCampaignId(campaignId);
        if (r == null) r = new CampaignResult();
        Integer totalSent = campaign.getSentCount();
        Integer qrUsed    = qrRedemptionRepository.countByCampaignId(campaignId);
        Double convRate   = calculateConversionRate(totalSent, qrUsed);
        r.setSentCount(totalSent);
        r.setRedeemedCount(qrUsed);
        r.setConversionRate(convRate);
        r.setAiSummary(buildCampaignSummary(campaignId, totalSent, qrUsed, convRate));
        r.setCreatedAt(LocalDateTime.now());
        CampaignResult saved = campaignResultRepository.save(r);
        campaign.setCampaignResult(saved);
        campaignRepository.save(campaign);
        return mapCampaignResult(saved);
    }

    // overload مع userId — للاستخدام من الكونترولر
    @Transactional
    public CampaignResultResponseOut generateCampaignResult(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        return generateCampaignResult(campaignId);
    }

    public void addCampaignResult(Integer userId, CampaignResultRequestIn dto) {
        validateCampaignResult(dto);
        Campaign campaign = checkCampaign(dto.getCampaignId());
        verifyOwnership(userId, campaign);
        CampaignResult r = new CampaignResult();
        setCampaignResult(r, dto);
        CampaignResult saved = campaignResultRepository.save(r);
        linkCampaign(saved, dto.getCampaignId());
    }

    public void updateCampaignResult(Integer userId, Integer campaignResultId, CampaignResultRequestIn dto) {
        validateCampaignResult(dto);
        CampaignResult old = checkCampaignResult(campaignResultId);
        verifyOwnership(userId, old.getCampaign());
        setCampaignResult(old, dto);
        CampaignResult saved = campaignResultRepository.save(old);
        linkCampaign(saved, dto.getCampaignId());
    }

    public void deleteCampaignResult(Integer userId, Integer campaignResultId) {
        CampaignResult r = checkCampaignResult(campaignResultId);
        verifyOwnership(userId, r.getCampaign());
        campaignResultRepository.delete(r);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void verifyOwnership(Integer userId, Campaign campaign) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null || campaign.getBranch() == null
                || !campaign.getBranch().getStore().getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("You do not have permission to access this resource");
    }

    private void setCampaignResult(CampaignResult r, CampaignResultRequestIn dto) {
        r.setSentCount(dto.getSentCount());
        r.setRedeemedCount(dto.getRedeemedCount());
        r.setConversionRate(dto.getConversionRate());
        r.setAiSummary(dto.getAiSummary());
        r.setCreatedAt(dto.getCreatedAt());
        r.setMonthlyReport(dto.getMonthlyReportId() == null ? null :
                monthlyReportRepository.findById(dto.getMonthlyReportId())
                        .orElseThrow(() -> new ApiException("Monthly report not found")));
    }

    private void validateCampaignResult(CampaignResultRequestIn dto) {
        if (dto.getRedeemedCount() > dto.getSentCount())
            throw new ApiException("Redeemed count cannot be greater than sent count");
        if (dto.getConversionRate() > 100) throw new ApiException("Conversion rate cannot be greater than 100");
        if (dto.getSentCount() == 0 && dto.getConversionRate() > 0)
            throw new ApiException("Conversion rate must be zero when sent count is zero");
    }

    private void linkCampaign(CampaignResult r, Integer campaignId) {
        if (campaignId == null) return;
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException("Campaign not found"));
        if (campaign.getCampaignResult() != null && !campaign.getCampaignResult().getId().equals(r.getId()))
            throw new ApiException("Campaign already has a campaign result");
        campaign.setCampaignResult(r);
        campaignRepository.save(campaign);
    }

    private CampaignResult checkCampaignResult(Integer id) {
        return campaignResultRepository.findById(id)
                .orElseThrow(() -> new ApiException("Campaign result not found"));
    }

    private Campaign checkCampaign(Integer id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ApiException("Campaign not found"));
    }

    private Double calculateConversionRate(Integer sent, Integer used) {
        if (sent == null || sent == 0) return 0.0;
        return (used * 100.0) / sent;
    }

    private String buildCampaignSummary(Integer campaignId, Integer totalSent, Integer qrUsed, Double convRate) {
        Integer answered = customerAnswerRepository.countByCampaignId(campaignId);
        Integer correct  = customerAnswerRepository.countByCampaignIdAndCorrect(campaignId, true);
        Integer wrong    = customerAnswerRepository.countByCampaignIdAndCorrect(campaignId, false);
        return "Sent: " + totalSent + ", answered: " + answered + ", correct: " + correct
                + ", wrong: " + wrong + ", QR used: " + qrUsed + ", conversion: " + convRate + "%";
    }

    private CampaignResultResponseOut mapCampaignResult(CampaignResult r) {
        CampaignResultResponseOut out = modelMapper.map(r, CampaignResultResponseOut.class);
        out.setCampaignId(r.getCampaign() == null ? null : r.getCampaign().getId());
        out.setMonthlyReportId(r.getMonthlyReport() == null ? null : r.getMonthlyReport().getId());
        return out;
    }
}