package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.CampaignSuggestionIn;
import com.example.fproject.DTO.OUT.CampaignSuggestionOut;
import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Enum.SubscriptionStatus;
import com.example.fproject.Enum.SuggestionStatus;
import com.example.fproject.Model.AIAnalysis;
import com.example.fproject.Model.CampaignSuggestion;
import com.example.fproject.Model.Subscription;
import com.example.fproject.Repository.AIAnalysisRepository;
import com.example.fproject.Repository.CampaignSuggestionRepository;
import com.example.fproject.Repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.fproject.Enum.SubscriptionPlanType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignSuggestionService {

    private final CampaignSuggestionRepository campaignSuggestionRepository;
    private final AIAnalysisRepository aiAnalysisRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OpenAiService openAiService;

    public List<CampaignSuggestionOut> getAllCampaignSuggestions() {
        List<CampaignSuggestion> campaignSuggestions = campaignSuggestionRepository.findAll();
        List<CampaignSuggestionOut> campaignSuggestionOuts = new ArrayList<>();

        for (CampaignSuggestion campaignSuggestion : campaignSuggestions) {
            campaignSuggestionOuts.add(convertToOut(campaignSuggestion));
        }

        return campaignSuggestionOuts;
    }

    public CampaignSuggestionOut getCampaignSuggestionById(Integer id) {
        CampaignSuggestion campaignSuggestion = campaignSuggestionRepository.findCampaignSuggestionById(id);

        if (campaignSuggestion == null) {
            throw new ApiException("Campaign suggestion not found");
        }

        return convertToOut(campaignSuggestion);
    }

    public List<CampaignSuggestionOut> getCampaignSuggestionsByAIAnalysisId(Integer aiAnalysisId) {
        AIAnalysis aiAnalysis = aiAnalysisRepository.findAIAnalysisById(aiAnalysisId);

        if (aiAnalysis == null) {
            throw new ApiException("AI analysis not found");
        }

        List<CampaignSuggestion> campaignSuggestions =
                campaignSuggestionRepository.findAllByAiAnalysis_Id(aiAnalysisId);

        List<CampaignSuggestionOut> campaignSuggestionOuts = new ArrayList<>();

        for (CampaignSuggestion campaignSuggestion : campaignSuggestions) {
            campaignSuggestionOuts.add(convertToOut(campaignSuggestion));
        }

        return campaignSuggestionOuts;
    }

    public List<CampaignSuggestionOut> generateCampaignSuggestions(Integer aiAnalysisId) {
        AIAnalysis aiAnalysis = aiAnalysisRepository.findAIAnalysisById(aiAnalysisId);

        if (aiAnalysis == null) {
            throw new ApiException("AI analysis not found");
        }

        Subscription activeSubscription = validateAIAnalysisReadyForSuggestion(aiAnalysis);

        List<CampaignSuggestion> oldSuggestions =
                campaignSuggestionRepository.findAllByAiAnalysis_Id(aiAnalysisId);

        if (oldSuggestions != null && !oldSuggestions.isEmpty()) {
            throw new ApiException("Campaign suggestions already generated for this AI analysis");
        }

        return generateAndSaveSuggestions(aiAnalysis, 1, activeSubscription);
    }

    public List<CampaignSuggestionOut> regenerateCampaignSuggestions(Integer aiAnalysisId) {
        AIAnalysis aiAnalysis = aiAnalysisRepository.findAIAnalysisById(aiAnalysisId);

        if (aiAnalysis == null) {
            throw new ApiException("AI analysis not found");
        }

        Subscription activeSubscription = validateAIAnalysisReadyForSuggestion(aiAnalysis);

        List<CampaignSuggestion> oldSuggestions =
                campaignSuggestionRepository.findAllByAiAnalysis_Id(aiAnalysisId);

        Integer latestRound = 0;

        for (CampaignSuggestion suggestion : oldSuggestions) {
            if (suggestion.getApprovalStatus() == SuggestionStatus.APPROVED) {
                throw new ApiException("Cannot regenerate suggestions because one suggestion is already approved");
            }

            if (suggestion.getSuggestionRound() > latestRound) {
                latestRound = suggestion.getSuggestionRound();
            }
        }

        if (latestRound == 0) {
            throw new ApiException("Generate campaign suggestions before regenerating");
        }

        Integer maxRounds = getMaxSuggestionRoundsByPlan(activeSubscription.getPlanType());

        if (latestRound >= maxRounds) {
            throw new ApiException("Maximum suggestion regeneration rounds reached for this subscription plan");
        }

        return generateAndSaveSuggestions(aiAnalysis, latestRound + 1, activeSubscription);
    }

    private List<CampaignSuggestionOut> generateAndSaveSuggestions(AIAnalysis aiAnalysis, Integer suggestionRound, Subscription activeSubscription) {
        String analysisSummary = buildAnalysisSummary(aiAnalysis);

        Integer suggestionCount = getSuggestionCountByPlan(activeSubscription.getPlanType());

        List<OpenAiService.CampaignSuggestionResult> aiResults =
                openAiService.generateCampaignSuggestionsFromAIAnalysis(analysisSummary, suggestionRound, suggestionCount);

        List<CampaignSuggestionOut> campaignSuggestionOuts = new ArrayList<>();

        for (OpenAiService.CampaignSuggestionResult result : aiResults) {
            validateGeneratedSuggestionTime(aiAnalysis, result);

            CampaignSuggestion campaignSuggestion = new CampaignSuggestion();

            campaignSuggestion.setTitle(result.title());
            campaignSuggestion.setDescription(result.description());
            campaignSuggestion.setOfferText(result.offerText());
            campaignSuggestion.setCampaignType(result.campaignType());
            campaignSuggestion.setSuggestedStartDate(result.suggestedStartDate());
            campaignSuggestion.setSuggestedEndDate(result.suggestedEndDate());
            campaignSuggestion.setSuggestedStartTime(result.suggestedStartTime());
            campaignSuggestion.setSuggestedEndTime(result.suggestedEndTime());
            campaignSuggestion.setTargetCustomersCount(result.targetCustomersCount());
            campaignSuggestion.setDiscountValue(result.discountValue());
            campaignSuggestion.setSuggestedProductName(result.suggestedProductName());
            campaignSuggestion.setSuggestionRound(result.suggestionRound());
            campaignSuggestion.setApprovalStatus(SuggestionStatus.PENDING);
            campaignSuggestion.setAiAnalysis(aiAnalysis);

            CampaignSuggestion savedSuggestion = campaignSuggestionRepository.save(campaignSuggestion);
            campaignSuggestionOuts.add(convertToOut(savedSuggestion));
        }

        return campaignSuggestionOuts;
    }

    private String buildAnalysisSummary(AIAnalysis aiAnalysis) {
        StringBuilder summary = new StringBuilder();

        summary.append("AI Analysis ID: ").append(aiAnalysis.getId()).append("\n");

        if (aiAnalysis.getSalesRecord() != null && aiAnalysis.getSalesRecord().getBranch() != null) {
            summary.append("Branch name: ")
                    .append(aiAnalysis.getSalesRecord().getBranch().getName())
                    .append("\n");

            summary.append("Branch opening time: ")
                    .append(aiAnalysis.getSalesRecord().getBranch().getOpeningTime())
                    .append("\n");

            summary.append("Branch closing time: ")
                    .append(aiAnalysis.getSalesRecord().getBranch().getClosingTime())
                    .append("\n");

            summary.append("Important rule: Campaign suggestions must be scheduled only inside branch working hours.\n");
            summary.append("Do not suggest campaigns before opening time or after closing time.\n");
        }

        summary.append("Top products: ").append(aiAnalysis.getTopProducts()).append("\n");
        summary.append("Low products: ").append(aiAnalysis.getLowProducts()).append("\n");
        summary.append("Peak hours: ").append(aiAnalysis.getPeakHours()).append("\n");
        summary.append("Slow hours: ").append(aiAnalysis.getSlowHours()).append("\n");
        summary.append("Surplus products: ").append(aiAnalysis.getSurplusProducts()).append("\n");
        summary.append("Seasonal patterns: ").append(aiAnalysis.getSeasonalPatterns()).append("\n");
        summary.append("Recommendation: ").append(aiAnalysis.getRecommendation()).append("\n");
        summary.append("AI summary: ").append(aiAnalysis.getAiSummary()).append("\n");

        return summary.toString();
    }

    private void validateGeneratedSuggestionTime(AIAnalysis aiAnalysis, OpenAiService.CampaignSuggestionResult result) {
        if (aiAnalysis.getSalesRecord() == null || aiAnalysis.getSalesRecord().getBranch() == null) {
            throw new ApiException("Branch not found for this AI analysis");
        }

        String openingTimeText = aiAnalysis.getSalesRecord().getBranch().getOpeningTime();
        String closingTimeText = aiAnalysis.getSalesRecord().getBranch().getClosingTime();

        if (openingTimeText == null || openingTimeText.isBlank()) {
            throw new ApiException("Branch opening time is missing");
        }

        if (closingTimeText == null || closingTimeText.isBlank()) {
            throw new ApiException("Branch closing time is missing");
        }

        LocalTime openingTime = LocalTime.parse(openingTimeText.trim());
        LocalTime closingTime = LocalTime.parse(closingTimeText.trim());

        if (result.suggestedStartDate().isBefore(LocalDate.now())) {
            throw new ApiException("AI suggested campaign start date cannot be in the past");
        }

        if (result.suggestedEndDate().isBefore(result.suggestedStartDate())) {
            throw new ApiException("AI suggested campaign end date cannot be before start date");
        }

        if (!result.suggestedEndTime().isAfter(result.suggestedStartTime())) {
            throw new ApiException("AI suggested campaign end time must be after start time");
        }

        if (result.suggestedStartTime().isBefore(openingTime)
                || result.suggestedEndTime().isAfter(closingTime)) {
            throw new ApiException("AI suggested campaign time is outside branch working hours");
        }
    }

    private Subscription validateAIAnalysisReadyForSuggestion(AIAnalysis aiAnalysis){
        if (aiAnalysis == null) {
            throw new ApiException("AI analysis not found");
        }

        if (aiAnalysis.getSalesRecord() == null) {
            throw new ApiException("Sales record not found for this AI analysis");
        }

        if (aiAnalysis.getSalesRecord().getBranch() == null) {
            throw new ApiException("Branch not found for this AI analysis");
        }

        if (aiAnalysis.getSalesRecord().getBranch().getStatus() != StoreStatus.ACTIVE) {
            throw new ApiException("Branch must be active before generating campaign suggestions");
        }

        if (aiAnalysis.getSalesRecord().getBranch().getStore() == null) {
            throw new ApiException("Store not found for this branch");
        }

        if (aiAnalysis.getSalesRecord().getBranch().getStore().getStatus() != StoreStatus.ACTIVE) {
            throw new ApiException("Store must be active before generating campaign suggestions");
        }

        if (aiAnalysis.getSalesRecord().getBranch().getStore().getStoreOwner() == null) {
            throw new ApiException("Store owner not found for this store");
        }

        Integer storeOwnerId = aiAnalysis.getSalesRecord().getBranch().getStore().getStoreOwner().getId();

        Subscription activeSubscription =
                subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        storeOwnerId,
                        SubscriptionStatus.ACTIVE
                );

        if (activeSubscription == null) {
            throw new ApiException("Store owner does not have an active subscription");
        }

        if (activeSubscription.getEndDate().isBefore(LocalDate.now())) {
            throw new ApiException("Store owner subscription is expired");
        }

        return activeSubscription;
    }

    private Integer getSuggestionCountByPlan(SubscriptionPlanType planType) {
        if (planType == SubscriptionPlanType.PROFESSIONAL_YEARLY) {
            return 5;
        }

        return 3;
    }

    private Integer getMaxSuggestionRoundsByPlan(SubscriptionPlanType planType) {
        if (planType == SubscriptionPlanType.BASIC_MONTHLY) {
            return 1;
        }

        return 3;
    }

    public void addCampaignSuggestion(CampaignSuggestionIn campaignSuggestionIn) {
        validateCampaignSuggestionIn(campaignSuggestionIn);

        AIAnalysis aiAnalysis = aiAnalysisRepository.findAIAnalysisById(campaignSuggestionIn.getAiAnalysisId());

        if (aiAnalysis == null) {
            throw new ApiException("AI analysis not found");
        }

        Boolean exists = campaignSuggestionRepository.existsByAiAnalysis_IdAndSuggestionRound(
                campaignSuggestionIn.getAiAnalysisId(),
                campaignSuggestionIn.getSuggestionRound()
        );

        if (Boolean.TRUE.equals(exists)) {
            throw new ApiException("Suggestion round already exists for this AI analysis");
        }

        CampaignSuggestion campaignSuggestion = new CampaignSuggestion();

        campaignSuggestion.setTitle(campaignSuggestionIn.getTitle());
        campaignSuggestion.setDescription(campaignSuggestionIn.getDescription());
        campaignSuggestion.setOfferText(campaignSuggestionIn.getOfferText());
        campaignSuggestion.setCampaignType(campaignSuggestionIn.getCampaignType());
        campaignSuggestion.setSuggestedStartDate(campaignSuggestionIn.getSuggestedStartDate());
        campaignSuggestion.setSuggestedEndDate(campaignSuggestionIn.getSuggestedEndDate());
        campaignSuggestion.setSuggestedStartTime(campaignSuggestionIn.getSuggestedStartTime());
        campaignSuggestion.setSuggestedEndTime(campaignSuggestionIn.getSuggestedEndTime());
        campaignSuggestion.setTargetCustomersCount(campaignSuggestionIn.getTargetCustomersCount());
        campaignSuggestion.setDiscountValue(campaignSuggestionIn.getDiscountValue());
        campaignSuggestion.setSuggestedProductName(campaignSuggestionIn.getSuggestedProductName());
        campaignSuggestion.setSuggestionRound(campaignSuggestionIn.getSuggestionRound());

        campaignSuggestion.setApprovalStatus(SuggestionStatus.PENDING);

        campaignSuggestion.setAiAnalysis(aiAnalysis);

        campaignSuggestionRepository.save(campaignSuggestion);
    }

    public void updateCampaignSuggestion(Integer id, CampaignSuggestionIn campaignSuggestionIn) {
        validateCampaignSuggestionIn(campaignSuggestionIn);

        CampaignSuggestion oldCampaignSuggestion =
                campaignSuggestionRepository.findCampaignSuggestionById(id);

        if (oldCampaignSuggestion == null) {
            throw new ApiException("Campaign suggestion not found");
        }

        if (oldCampaignSuggestion.getCampaign() != null) {
            throw new ApiException("Cannot update campaign suggestion because it is linked to a campaign");
        }

        AIAnalysis aiAnalysis = aiAnalysisRepository.findAIAnalysisById(campaignSuggestionIn.getAiAnalysisId());

        if (aiAnalysis == null) {
            throw new ApiException("AI analysis not found");
        }

        Boolean changedAIAnalysis =
                !oldCampaignSuggestion.getAiAnalysis().getId().equals(campaignSuggestionIn.getAiAnalysisId());

        Boolean changedSuggestionRound =
                !oldCampaignSuggestion.getSuggestionRound().equals(campaignSuggestionIn.getSuggestionRound());

        if (changedAIAnalysis || changedSuggestionRound) {
            Boolean exists = campaignSuggestionRepository.existsByAiAnalysis_IdAndSuggestionRound(
                    campaignSuggestionIn.getAiAnalysisId(),
                    campaignSuggestionIn.getSuggestionRound()
            );

            if (Boolean.TRUE.equals(exists)) {
                throw new ApiException("Suggestion round already exists for this AI analysis");
            }
        }

        oldCampaignSuggestion.setTitle(campaignSuggestionIn.getTitle());
        oldCampaignSuggestion.setDescription(campaignSuggestionIn.getDescription());
        oldCampaignSuggestion.setOfferText(campaignSuggestionIn.getOfferText());
        oldCampaignSuggestion.setCampaignType(campaignSuggestionIn.getCampaignType());
        oldCampaignSuggestion.setSuggestedStartDate(campaignSuggestionIn.getSuggestedStartDate());
        oldCampaignSuggestion.setSuggestedEndDate(campaignSuggestionIn.getSuggestedEndDate());
        oldCampaignSuggestion.setSuggestedStartTime(campaignSuggestionIn.getSuggestedStartTime());
        oldCampaignSuggestion.setSuggestedEndTime(campaignSuggestionIn.getSuggestedEndTime());
        oldCampaignSuggestion.setTargetCustomersCount(campaignSuggestionIn.getTargetCustomersCount());
        oldCampaignSuggestion.setDiscountValue(campaignSuggestionIn.getDiscountValue());
        oldCampaignSuggestion.setSuggestedProductName(campaignSuggestionIn.getSuggestedProductName());
        oldCampaignSuggestion.setSuggestionRound(campaignSuggestionIn.getSuggestionRound());
        oldCampaignSuggestion.setAiAnalysis(aiAnalysis);

        campaignSuggestionRepository.save(oldCampaignSuggestion);
    }

    public void deleteCampaignSuggestion(Integer id) {
        CampaignSuggestion campaignSuggestion = campaignSuggestionRepository.findCampaignSuggestionById(id);

        if (campaignSuggestion == null) {
            throw new ApiException("Campaign suggestion not found");
        }

        if (campaignSuggestion.getCampaign() != null) {
            throw new ApiException("Cannot delete campaign suggestion because it is linked to a campaign");
        }

        if (campaignSuggestion.getApprovalStatus() == SuggestionStatus.APPROVED) {
            throw new ApiException("Cannot delete approved campaign suggestion");
        }

        campaignSuggestionRepository.delete(campaignSuggestion);
    }

    public CampaignSuggestionOut getApprovedSuggestionByAnalysis(Integer analysisId) {
        AIAnalysis aiAnalysis = aiAnalysisRepository.findAIAnalysisById(analysisId);

        if (aiAnalysis == null) {
            throw new ApiException("AI analysis not found");
        }

        List<CampaignSuggestion> suggestions =
                campaignSuggestionRepository.findAllByAiAnalysis_Id(analysisId);

        for (CampaignSuggestion suggestion : suggestions) {
            if (suggestion.getApprovalStatus() == SuggestionStatus.APPROVED) {
                return convertToOut(suggestion);
            }
        }

        throw new ApiException("No approved campaign suggestion found for this AI analysis");
    }

    public List<CampaignSuggestionOut> getPendingSuggestionsByAnalysis(Integer analysisId) {
        AIAnalysis aiAnalysis = aiAnalysisRepository.findAIAnalysisById(analysisId);

        if (aiAnalysis == null) {
            throw new ApiException("AI analysis not found");
        }

        List<CampaignSuggestion> suggestions =
                campaignSuggestionRepository.findAllByAiAnalysis_Id(analysisId);

        List<CampaignSuggestionOut> pendingSuggestions = new ArrayList<>();

        for (CampaignSuggestion suggestion : suggestions) {
            if (suggestion.getApprovalStatus() == SuggestionStatus.PENDING) {
                pendingSuggestions.add(convertToOut(suggestion));
            }
        }

        return pendingSuggestions;
    }

    public void approveCampaignSuggestion(Integer id) {
        CampaignSuggestion campaignSuggestion = campaignSuggestionRepository.findCampaignSuggestionById(id);

        if (campaignSuggestion == null) {
            throw new ApiException("Campaign suggestion not found");
        }

        validateAIAnalysisReadyForSuggestion(campaignSuggestion.getAiAnalysis());

        if (campaignSuggestion.getApprovalStatus() == SuggestionStatus.APPROVED) {
            throw new ApiException("Campaign suggestion is already approved");
        }

        if (campaignSuggestion.getApprovalStatus() == SuggestionStatus.REJECTED) {
            throw new ApiException("Rejected campaign suggestion cannot be approved");
        }

        List<CampaignSuggestion> suggestions =
                campaignSuggestionRepository.findAllByAiAnalysis_Id(campaignSuggestion.getAiAnalysis().getId());

        for (CampaignSuggestion suggestion : suggestions) {
            if (!suggestion.getId().equals(campaignSuggestion.getId())
                    && suggestion.getApprovalStatus() == SuggestionStatus.APPROVED) {
                throw new ApiException("Another campaign suggestion is already approved for this AI analysis");
            }
        }

        campaignSuggestion.setApprovalStatus(SuggestionStatus.APPROVED);
        campaignSuggestionRepository.save(campaignSuggestion);
    }

    public void rejectCampaignSuggestion(Integer id) {
        CampaignSuggestion campaignSuggestion = campaignSuggestionRepository.findCampaignSuggestionById(id);

        if (campaignSuggestion == null) {
            throw new ApiException("Campaign suggestion not found");
        }

        validateAIAnalysisReadyForSuggestion(campaignSuggestion.getAiAnalysis());

        if (campaignSuggestion.getCampaign() != null) {
            throw new ApiException("Cannot reject campaign suggestion because it is linked to a campaign");
        }

        if (campaignSuggestion.getApprovalStatus() == SuggestionStatus.APPROVED) {
            throw new ApiException("Approved campaign suggestion cannot be rejected");
        }

        if (campaignSuggestion.getApprovalStatus() == SuggestionStatus.REJECTED) {
            throw new ApiException("Campaign suggestion is already rejected");
        }

        campaignSuggestion.setApprovalStatus(SuggestionStatus.REJECTED);
        campaignSuggestionRepository.save(campaignSuggestion);
    }

    private void validateCampaignSuggestionIn(CampaignSuggestionIn campaignSuggestionIn) {
        if (campaignSuggestionIn.getTitle() == null || campaignSuggestionIn.getTitle().isBlank()) {
            throw new ApiException("Title is required");
        }

        if (campaignSuggestionIn.getOfferText() == null || campaignSuggestionIn.getOfferText().isBlank()) {
            throw new ApiException("Offer text is required");
        }

        if (campaignSuggestionIn.getCampaignType() == null) {
            throw new ApiException("Campaign type is required");
        }

        if (campaignSuggestionIn.getSuggestedStartTime() == null) {
            throw new ApiException("Suggested start time is required");
        }

        if (campaignSuggestionIn.getSuggestedEndTime() == null) {
            throw new ApiException("Suggested end time is required");
        }

        if (campaignSuggestionIn.getSuggestedStartDate() == null) {
            throw new ApiException("Suggested start date is required");
        }

        if (campaignSuggestionIn.getSuggestedEndDate() == null) {
            throw new ApiException("Suggested end date is required");
        }

        if (campaignSuggestionIn.getSuggestedEndDate().isBefore(campaignSuggestionIn.getSuggestedStartDate())) {
            throw new ApiException("Suggested end date must not be before suggested start date");
        }

        if (!campaignSuggestionIn.getSuggestedEndTime().isAfter(campaignSuggestionIn.getSuggestedStartTime())) {
            throw new ApiException("Suggested end time must be after suggested start time");
        }

        if (campaignSuggestionIn.getTargetCustomersCount() == null) {
            throw new ApiException("Target customers count is required");
        }

        if (campaignSuggestionIn.getTargetCustomersCount() < 0) {
            throw new ApiException("Target customers count cannot be negative");
        }

        if (campaignSuggestionIn.getDiscountValue() == null) {
            throw new ApiException("Discount value is required");
        }

        if (campaignSuggestionIn.getDiscountValue() < 0 || campaignSuggestionIn.getDiscountValue() > 100) {
            throw new ApiException("Discount value must be between 0 and 100");
        }

        if (campaignSuggestionIn.getSuggestedProductName() == null
                || campaignSuggestionIn.getSuggestedProductName().isBlank()) {
            throw new ApiException("Suggested product name is required");
        }

        if (campaignSuggestionIn.getSuggestionRound() == null) {
            throw new ApiException("Suggestion round is required");
        }

        if (campaignSuggestionIn.getSuggestionRound() <= 0) {
            throw new ApiException("Suggestion round must be greater than zero");
        }

        if (campaignSuggestionIn.getAiAnalysisId() == null) {
            throw new ApiException("AI analysis id is required");
        }
    }

    private CampaignSuggestionOut convertToOut(CampaignSuggestion campaignSuggestion) {
        Integer campaignId = null;

        if (campaignSuggestion.getCampaign() != null) {
            campaignId = campaignSuggestion.getCampaign().getId();
        }

        return new CampaignSuggestionOut(
                campaignSuggestion.getId(),
                campaignSuggestion.getTitle(),
                campaignSuggestion.getDescription(),
                campaignSuggestion.getOfferText(),
                campaignSuggestion.getCampaignType(),
                campaignSuggestion.getSuggestedStartTime(),
                campaignSuggestion.getSuggestedEndTime(),
                campaignSuggestion.getSuggestedStartDate(),
                campaignSuggestion.getSuggestedEndDate(),
                campaignSuggestion.getTargetCustomersCount(),
                campaignSuggestion.getDiscountValue(),
                campaignSuggestion.getSuggestedProductName(),
                campaignSuggestion.getApprovalStatus(),
                campaignSuggestion.getSuggestionRound(),
                campaignSuggestion.getAiAnalysis().getId(),
                campaignId
        );
    }
}
