package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.CampaignSuggestionIn;
import com.example.fproject.DTO.OUT.CampaignSuggestionOut;
import com.example.fproject.Enum.SuggestionStatus;
import com.example.fproject.Model.AIAnalysis;
import com.example.fproject.Model.CampaignSuggestion;
import com.example.fproject.Repository.AIAnalysisRepository;
import com.example.fproject.Repository.CampaignSuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignSuggestionService {

    private final CampaignSuggestionRepository campaignSuggestionRepository;
    private final AIAnalysisRepository aiAnalysisRepository;

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
        campaignSuggestion.setSuggestedStartTime(campaignSuggestionIn.getSuggestedStartTime());
        campaignSuggestion.setSuggestedEndTime(campaignSuggestionIn.getSuggestedEndTime());
        campaignSuggestion.setTargetCustomersCount(campaignSuggestionIn.getTargetCustomersCount());
        campaignSuggestion.setDiscountValue(campaignSuggestionIn.getDiscountValue());
        campaignSuggestion.setSuggestedProductName(campaignSuggestionIn.getSuggestedProductName());
        campaignSuggestion.setSuggestionRound(campaignSuggestionIn.getSuggestionRound());

        // اليوزر ما يدخل الحالة، السيرفس يبدأها Pending
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
        oldCampaignSuggestion.setSuggestedStartTime(campaignSuggestionIn.getSuggestedStartTime());
        oldCampaignSuggestion.setSuggestedEndTime(campaignSuggestionIn.getSuggestedEndTime());
        oldCampaignSuggestion.setTargetCustomersCount(campaignSuggestionIn.getTargetCustomersCount());
        oldCampaignSuggestion.setDiscountValue(campaignSuggestionIn.getDiscountValue());
        oldCampaignSuggestion.setSuggestedProductName(campaignSuggestionIn.getSuggestedProductName());
        oldCampaignSuggestion.setSuggestionRound(campaignSuggestionIn.getSuggestionRound());
        oldCampaignSuggestion.setAiAnalysis(aiAnalysis);

        // ما نغير approvalStatus من update
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

    public void approveCampaignSuggestion(Integer id) {
        CampaignSuggestion campaignSuggestion = campaignSuggestionRepository.findCampaignSuggestionById(id);

        if (campaignSuggestion == null) {
            throw new ApiException("Campaign suggestion not found");
        }

        if (campaignSuggestion.getApprovalStatus() == SuggestionStatus.APPROVED) {
            throw new ApiException("Campaign suggestion is already approved");
        }

        if (campaignSuggestion.getApprovalStatus() == SuggestionStatus.REJECTED) {
            throw new ApiException("Rejected campaign suggestion cannot be approved");
        }

        campaignSuggestion.setApprovalStatus(SuggestionStatus.APPROVED);
        campaignSuggestionRepository.save(campaignSuggestion);
    }

    public void rejectCampaignSuggestion(Integer id) {
        CampaignSuggestion campaignSuggestion = campaignSuggestionRepository.findCampaignSuggestionById(id);

        if (campaignSuggestion == null) {
            throw new ApiException("Campaign suggestion not found");
        }

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
