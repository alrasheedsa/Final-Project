package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.CampaignSuggestionIn;
import com.example.fproject.DTO.OUT.CampaignSuggestionOut;
import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Enum.SubscriptionPlanType;
import com.example.fproject.Enum.SubscriptionStatus;
import com.example.fproject.Enum.SuggestionStatus;
import com.example.fproject.Model.AIAnalysis;
import com.example.fproject.Model.CampaignSuggestion;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Model.Subscription;
import com.example.fproject.Repository.AIAnalysisRepository;
import com.example.fproject.Repository.CampaignSuggestionRepository;
import com.example.fproject.Repository.StoreOwnerRepository;
import com.example.fproject.Repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignSuggestionService {

    private final CampaignSuggestionRepository campaignSuggestionRepository;
    private final AIAnalysisRepository aiAnalysisRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OpenAiService openAiService;
    private final HolidayService holidayService;
    private final EmailService emailService;

    public List<CampaignSuggestionOut> getAllCampaignSuggestions() {
        List<CampaignSuggestion> list = campaignSuggestionRepository.findAll();
        List<CampaignSuggestionOut> result = new ArrayList<>();
        for (CampaignSuggestion s : list) result.add(convertToOut(s));
        return result;
    }

    public CampaignSuggestionOut getCampaignSuggestionById(Integer userId, Integer id) {
        CampaignSuggestion s = findSuggestionOrThrow(id);
        verifyOwnership(userId, s);
        return convertToOut(s);
    }

    public List<CampaignSuggestionOut> getCampaignSuggestionsByAIAnalysisId(Integer userId, Integer aiAnalysisId) {
        AIAnalysis aiAnalysis = findAnalysisOrThrow(aiAnalysisId);
        verifyOwnershipByAnalysis(userId, aiAnalysis);
        List<CampaignSuggestionOut> result = new ArrayList<>();
        for (CampaignSuggestion s : campaignSuggestionRepository.findAllByAiAnalysis_Id(aiAnalysisId))
            result.add(convertToOut(s));
        return result;
    }

    public List<CampaignSuggestionOut> generateCampaignSuggestions(Integer userId, Integer aiAnalysisId) {
        AIAnalysis aiAnalysis = findAnalysisOrThrow(aiAnalysisId);
        verifyOwnershipByAnalysis(userId, aiAnalysis);
        Subscription activeSubscription = validateAIAnalysisReadyForSuggestion(aiAnalysis);
        List<CampaignSuggestion> old = campaignSuggestionRepository.findAllByAiAnalysis_Id(aiAnalysisId);
        if (old != null && !old.isEmpty())
            throw new ApiException("Campaign suggestions already generated for this AI analysis");
        return generateAndSaveSuggestions(aiAnalysis, 1, activeSubscription);
    }

    public List<CampaignSuggestionOut> regenerateCampaignSuggestions(Integer userId, Integer aiAnalysisId) {
        AIAnalysis aiAnalysis = findAnalysisOrThrow(aiAnalysisId);
        verifyOwnershipByAnalysis(userId, aiAnalysis);
        Subscription activeSubscription = validateAIAnalysisReadyForSuggestion(aiAnalysis);
        List<CampaignSuggestion> old = campaignSuggestionRepository.findAllByAiAnalysis_Id(aiAnalysisId);
        Integer latestRound = 0;
        for (CampaignSuggestion s : old) {
            if (s.getApprovalStatus() == SuggestionStatus.APPROVED)
                throw new ApiException("Cannot regenerate suggestions because one suggestion is already approved");
            if (s.getSuggestionRound() > latestRound) latestRound = s.getSuggestionRound();
        }
        if (latestRound == 0) throw new ApiException("Generate campaign suggestions before regenerating");
        int maxRounds = getMaxSuggestionRoundsByPlan(activeSubscription.getPlanType());
        if (latestRound >= maxRounds)
            throw new ApiException("Maximum suggestion regeneration rounds reached for this subscription plan");
        return generateAndSaveSuggestions(aiAnalysis, latestRound + 1, activeSubscription);
    }

    public void addCampaignSuggestion(Integer userId, Integer aiAnalysisId, CampaignSuggestionIn dto) {
        validateCampaignSuggestionIn(dto);
        AIAnalysis aiAnalysis = findAnalysisOrThrow(aiAnalysisId);
        verifyOwnershipByAnalysis(userId, aiAnalysis);

        CampaignSuggestion s = new CampaignSuggestion();
        s.setTitle(dto.getTitle());
        s.setDescription(dto.getDescription());
        s.setCampaignType(dto.getCampaignType());
        s.setOfferText(dto.getOfferText());
        s.setSuggestedProductName(dto.getSuggestedProductName());
        s.setSuggestedStartDate(dto.getSuggestedStartDate());
        s.setSuggestedEndDate(dto.getSuggestedEndDate());
        s.setSuggestedStartTime(dto.getSuggestedStartTime());
        s.setSuggestedEndTime(dto.getSuggestedEndTime());
        s.setTargetCustomersCount(dto.getTargetCustomersCount());
        s.setDiscountValue(dto.getDiscountValue());
        s.setApprovalStatus(SuggestionStatus.PENDING);
        s.setSuggestionRound(1);
        s.setAiAnalysis(aiAnalysis);
        campaignSuggestionRepository.save(s);
    }

    public void updateCampaignSuggestion(Integer userId, Integer id, Integer aiAnalysisId, CampaignSuggestionIn dto) {
        validateCampaignSuggestionIn(dto);
        CampaignSuggestion old = findSuggestionOrThrow(id);
        verifyOwnership(userId, old);
        AIAnalysis aiAnalysis = findAnalysisOrThrow(aiAnalysisId);
        verifyOwnershipByAnalysis(userId, aiAnalysis);
        if (old.getApprovalStatus() == SuggestionStatus.APPROVED)
            throw new ApiException("Approved campaign suggestion cannot be updated");

        old.setTitle(dto.getTitle());
        old.setDescription(dto.getDescription());
        old.setCampaignType(dto.getCampaignType());
        old.setOfferText(dto.getOfferText());
        old.setSuggestedProductName(dto.getSuggestedProductName());
        old.setSuggestedStartDate(dto.getSuggestedStartDate());
        old.setSuggestedEndDate(dto.getSuggestedEndDate());
        old.setSuggestedStartTime(dto.getSuggestedStartTime());
        old.setSuggestedEndTime(dto.getSuggestedEndTime());
        old.setTargetCustomersCount(dto.getTargetCustomersCount());
        old.setDiscountValue(dto.getDiscountValue());
        old.setAiAnalysis(aiAnalysis);
        campaignSuggestionRepository.save(old);
    }

    public void deleteCampaignSuggestion(Integer userId, Integer id) {
        CampaignSuggestion s = findSuggestionOrThrow(id);
        verifyOwnership(userId, s);
        if (s.getCampaign() != null)
            throw new ApiException("Cannot delete campaign suggestion because it is linked to a campaign");
        if (s.getApprovalStatus() == SuggestionStatus.APPROVED)
            throw new ApiException("Cannot delete approved campaign suggestion");
        campaignSuggestionRepository.delete(s);
    }

    public CampaignSuggestionOut getApprovedSuggestionByAnalysis(Integer userId, Integer analysisId) {
        AIAnalysis aiAnalysis = findAnalysisOrThrow(analysisId);
        verifyOwnershipByAnalysis(userId, aiAnalysis);
        for (CampaignSuggestion s : campaignSuggestionRepository.findAllByAiAnalysis_Id(analysisId)) {
            if (s.getApprovalStatus() == SuggestionStatus.APPROVED) return convertToOut(s);
        }
        throw new ApiException("No approved campaign suggestion found for this AI analysis");
    }

    public List<CampaignSuggestionOut> getPendingSuggestionsByAnalysis(Integer userId, Integer analysisId) {
        AIAnalysis aiAnalysis = findAnalysisOrThrow(analysisId);
        verifyOwnershipByAnalysis(userId, aiAnalysis);
        List<CampaignSuggestionOut> result = new ArrayList<>();
        for (CampaignSuggestion s : campaignSuggestionRepository.findAllByAiAnalysis_Id(analysisId)) {
            if (s.getApprovalStatus() == SuggestionStatus.PENDING) result.add(convertToOut(s));
        }
        return result;
    }

    public void approveCampaignSuggestion(Integer userId, Integer id) {
        CampaignSuggestion s = findSuggestionOrThrow(id);
        verifyOwnership(userId, s);
        validateAIAnalysisReadyForSuggestion(s.getAiAnalysis());
        if (s.getApprovalStatus() == SuggestionStatus.APPROVED) throw new ApiException("Campaign suggestion is already approved");
        if (s.getApprovalStatus() == SuggestionStatus.REJECTED) throw new ApiException("Rejected campaign suggestion cannot be approved");
        for (CampaignSuggestion other : campaignSuggestionRepository.findAllByAiAnalysis_Id(s.getAiAnalysis().getId())) {
            if (!other.getId().equals(s.getId()) && other.getApprovalStatus() == SuggestionStatus.APPROVED)
                throw new ApiException("Another campaign suggestion is already approved for this AI analysis");
        }
        s.setApprovalStatus(SuggestionStatus.APPROVED);
        CampaignSuggestion saved = campaignSuggestionRepository.save(s);
        sendApprovedSuggestionEmailSafely(saved);
    }

    public String sendApprovedCampaignSuggestionEmail(Integer userId, Integer suggestionId) {
        CampaignSuggestion s = findSuggestionOrThrow(suggestionId);
        verifyOwnership(userId, s);
        if (s.getApprovalStatus() != SuggestionStatus.APPROVED)
            throw new ApiException("Campaign suggestion must be approved before sending approval email");
        return sendApprovedSuggestionEmail(s);
    }

    public void rejectCampaignSuggestion(Integer userId, Integer id) {
        CampaignSuggestion s = findSuggestionOrThrow(id);
        verifyOwnership(userId, s);
        validateAIAnalysisReadyForSuggestion(s.getAiAnalysis());
        if (s.getCampaign() != null) throw new ApiException("Cannot reject campaign suggestion because it is linked to a campaign");
        if (s.getApprovalStatus() == SuggestionStatus.APPROVED) throw new ApiException("Approved campaign suggestion cannot be rejected");
        if (s.getApprovalStatus() == SuggestionStatus.REJECTED) throw new ApiException("Campaign suggestion is already rejected");
        s.setApprovalStatus(SuggestionStatus.REJECTED);
        campaignSuggestionRepository.save(s);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void verifyOwnership(Integer userId, CampaignSuggestion s) {
        verifyOwnershipByAnalysis(userId, s.getAiAnalysis());
    }

    private void verifyOwnershipByAnalysis(Integer userId, AIAnalysis aiAnalysis) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null || !aiAnalysis.getSalesRecord().getBranch()
                .getStore().getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("You do not have permission to access this resource");
    }

    private CampaignSuggestion findSuggestionOrThrow(Integer id) {
        CampaignSuggestion s = campaignSuggestionRepository.findCampaignSuggestionById(id);
        if (s == null) throw new ApiException("Campaign suggestion not found");
        return s;
    }

    private AIAnalysis findAnalysisOrThrow(Integer id) {
        AIAnalysis a = aiAnalysisRepository.findAIAnalysisById(id);
        if (a == null) throw new ApiException("AI analysis not found");
        return a;
    }

    private Subscription validateAIAnalysisReadyForSuggestion(AIAnalysis aiAnalysis) {
        if (aiAnalysis.getSalesRecord() == null) throw new ApiException("Sales record not found for this AI analysis");
        if (aiAnalysis.getSalesRecord().getBranch() == null) throw new ApiException("Branch not found for this AI analysis");
        if (aiAnalysis.getSalesRecord().getBranch().getStatus() != StoreStatus.ACTIVE)
            throw new ApiException("Branch must be active before generating campaign suggestions");
        if (aiAnalysis.getSalesRecord().getBranch().getStore() == null) throw new ApiException("Store not found for this branch");
        if (aiAnalysis.getSalesRecord().getBranch().getStore().getStatus() != StoreStatus.ACTIVE)
            throw new ApiException("Store must be active before generating campaign suggestions");
        if (aiAnalysis.getSalesRecord().getBranch().getStore().getStoreOwner() == null)
            throw new ApiException("Store owner not found for this store");
        Integer storeOwnerId = aiAnalysis.getSalesRecord().getBranch().getStore().getStoreOwner().getId();
        Subscription active = subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(storeOwnerId, SubscriptionStatus.ACTIVE);
        if (active == null) throw new ApiException("Store owner does not have an active subscription");
        if (active.getEndDate().isBefore(LocalDate.now())) throw new ApiException("Store owner subscription is expired");
        return active;
    }

    private List<CampaignSuggestionOut> generateAndSaveSuggestions(AIAnalysis aiAnalysis, Integer round, Subscription subscription) {
        String summary = buildAnalysisSummary(aiAnalysis);
        int count = getSuggestionCountByPlan(subscription.getPlanType());
        List<OpenAiService.CampaignSuggestionResult> aiResults =
                openAiService.generateCampaignSuggestionsFromAIAnalysis(summary, round, count);
        List<CampaignSuggestionOut> result = new ArrayList<>();
        for (OpenAiService.CampaignSuggestionResult r : aiResults) {
            validateGeneratedSuggestionTime(aiAnalysis, r);
            CampaignSuggestion s = new CampaignSuggestion();
            s.setTitle(r.title()); s.setDescription(r.description()); s.setOfferText(r.offerText());
            s.setCampaignType(r.campaignType()); s.setSuggestedStartDate(r.suggestedStartDate());
            s.setSuggestedEndDate(r.suggestedEndDate()); s.setSuggestedStartTime(r.suggestedStartTime());
            s.setSuggestedEndTime(r.suggestedEndTime()); s.setTargetCustomersCount(r.targetCustomersCount());
            s.setDiscountValue(r.discountValue()); s.setSuggestedProductName(r.suggestedProductName());
            s.setSuggestionRound(r.suggestionRound()); s.setApprovalStatus(SuggestionStatus.PENDING);
            s.setAiAnalysis(aiAnalysis);
            result.add(convertToOut(campaignSuggestionRepository.save(s)));
        }
        return result;
    }

    private String buildAnalysisSummary(AIAnalysis aiAnalysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("AI Analysis ID: ").append(aiAnalysis.getId()).append("\n");
        if (aiAnalysis.getSalesRecord() != null && aiAnalysis.getSalesRecord().getBranch() != null) {
            sb.append("Branch name: ").append(aiAnalysis.getSalesRecord().getBranch().getName()).append("\n");
            sb.append("Branch opening time: ").append(aiAnalysis.getSalesRecord().getBranch().getOpeningTime()).append("\n");
            sb.append("Branch closing time: ").append(aiAnalysis.getSalesRecord().getBranch().getClosingTime()).append("\n");
            sb.append("Important rule: Campaign suggestions must be scheduled only inside branch working hours.\n");
        }
        sb.append("Top products: ").append(aiAnalysis.getTopProducts()).append("\n");
        sb.append("Low products: ").append(aiAnalysis.getLowProducts()).append("\n");
        sb.append("Peak hours: ").append(aiAnalysis.getPeakHours()).append("\n");
        sb.append("Slow hours: ").append(aiAnalysis.getSlowHours()).append("\n");
        sb.append("Surplus products: ").append(aiAnalysis.getSurplusProducts()).append("\n");
        sb.append("Seasonal patterns: ").append(aiAnalysis.getSeasonalPatterns()).append("\n");
        sb.append("Recommendation: ").append(aiAnalysis.getRecommendation()).append("\n");
        sb.append("AI summary: ").append(aiAnalysis.getAiSummary()).append("\n");
        sb.append("\nHoliday context:\n").append(holidayService.getSaudiHolidayContextForAI()).append("\n");
        return sb.toString();
    }

    private void validateGeneratedSuggestionTime(AIAnalysis aiAnalysis, OpenAiService.CampaignSuggestionResult result) {
        if (aiAnalysis.getSalesRecord() == null || aiAnalysis.getSalesRecord().getBranch() == null)
            throw new ApiException("Branch not found for this AI analysis");
        String openingText = aiAnalysis.getSalesRecord().getBranch().getOpeningTime();
        String closingText = aiAnalysis.getSalesRecord().getBranch().getClosingTime();
        if (openingText == null || openingText.isBlank()) throw new ApiException("Branch opening time is missing");
        if (closingText == null || closingText.isBlank()) throw new ApiException("Branch closing time is missing");
        LocalTime opening = LocalTime.parse(openingText.trim());
        LocalTime closing = LocalTime.parse(closingText.trim());
        if (result.suggestedStartDate().isBefore(LocalDate.now()))
            throw new ApiException("AI suggested campaign start date cannot be in the past");
        if (result.suggestedEndDate().isBefore(result.suggestedStartDate()))
            throw new ApiException("AI suggested campaign end date cannot be before start date");
        if (!result.suggestedEndTime().isAfter(result.suggestedStartTime()))
            throw new ApiException("AI suggested campaign end time must be after start time");
        if (result.suggestedStartTime().isBefore(opening) || result.suggestedEndTime().isAfter(closing))
            throw new ApiException("AI suggested campaign time is outside branch working hours");
    }

    private int getSuggestionCountByPlan(SubscriptionPlanType planType) {
        return planType == SubscriptionPlanType.PROFESSIONAL_YEARLY ? 5 : 3;
    }

    private int getMaxSuggestionRoundsByPlan(SubscriptionPlanType planType) {
        return planType == SubscriptionPlanType.BASIC_MONTHLY ? 1 : 3;
    }

    private void validateCampaignSuggestionIn(CampaignSuggestionIn dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) throw new ApiException("Title is required");
        if (dto.getOfferText() == null || dto.getOfferText().isBlank()) throw new ApiException("Offer text is required");
        if (dto.getCampaignType() == null) throw new ApiException("Campaign type is required");
        if (dto.getSuggestedStartTime() == null) throw new ApiException("Suggested start time is required");
        if (dto.getSuggestedEndTime() == null) throw new ApiException("Suggested end time is required");
        if (dto.getSuggestedStartDate() == null) throw new ApiException("Suggested start date is required");
        if (dto.getSuggestedEndDate() == null) throw new ApiException("Suggested end date is required");
        if (dto.getSuggestedEndDate().isBefore(dto.getSuggestedStartDate()))
            throw new ApiException("Suggested end date must not be before suggested start date");
        if (!dto.getSuggestedEndTime().isAfter(dto.getSuggestedStartTime()))
            throw new ApiException("Suggested end time must be after suggested start time");
        if (dto.getTargetCustomersCount() == null) throw new ApiException("Target customers count is required");
        if (dto.getTargetCustomersCount() < 0) throw new ApiException("Target customers count cannot be negative");
        if (dto.getDiscountValue() == null) throw new ApiException("Discount value is required");
        if (dto.getDiscountValue() < 0 || dto.getDiscountValue() > 100)
            throw new ApiException("Discount value must be between 0 and 100");
        if (dto.getSuggestedProductName() == null || dto.getSuggestedProductName().isBlank())
            throw new ApiException("Suggested product name is required");
    }

    private void sendApprovedSuggestionEmailSafely(CampaignSuggestion s) {
        try { sendApprovedSuggestionEmail(s); }
        catch (Exception e) { System.out.println("Campaign suggestion approval email not sent: " + e.getMessage()); }
    }

    private String sendApprovedSuggestionEmail(CampaignSuggestion s) {
        if (s.getAiAnalysis() == null || s.getAiAnalysis().getSalesRecord() == null
                || s.getAiAnalysis().getSalesRecord().getBranch() == null
                || s.getAiAnalysis().getSalesRecord().getBranch().getStore() == null
                || s.getAiAnalysis().getSalesRecord().getBranch().getStore().getStoreOwner() == null
                || s.getAiAnalysis().getSalesRecord().getBranch().getStore().getStoreOwner().getUser() == null)
            throw new ApiException("Store owner email information not found");
        String ownerEmail = s.getAiAnalysis().getSalesRecord().getBranch().getStore().getStoreOwner().getUser().getEmail();
        String ownerName  = s.getAiAnalysis().getSalesRecord().getBranch().getStore().getStoreOwner().getUser().getFullName();
        String branchName = s.getAiAnalysis().getSalesRecord().getBranch().getName();
        return emailService.sendCampaignSuggestionApprovedEmail(
                ownerEmail, ownerName, branchName,
                s.getTitle(), s.getCampaignType().name(), s.getOfferText(),
                s.getSuggestedProductName(),
                s.getSuggestedStartDate().toString(), s.getSuggestedEndDate().toString(),
                s.getSuggestedStartTime().toString(), s.getSuggestedEndTime().toString(),
                s.getTargetCustomersCount(), s.getDiscountValue()
        );
    }

    private CampaignSuggestionOut convertToOut(CampaignSuggestion s) {
        return new CampaignSuggestionOut(
                s.getId(), s.getTitle(), s.getDescription(), s.getOfferText(),
                s.getCampaignType(), s.getSuggestedStartTime(), s.getSuggestedEndTime(),
                s.getSuggestedStartDate(), s.getSuggestedEndDate(),
                s.getTargetCustomersCount(), s.getDiscountValue(), s.getSuggestedProductName(),
                s.getApprovalStatus(), s.getSuggestionRound(), s.getAiAnalysis().getId(),
                s.getCampaign() != null ? s.getCampaign().getId() : null
        );
    }
}