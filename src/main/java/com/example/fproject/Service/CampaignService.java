package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.CampaignRequestIn;
import com.example.fproject.DTO.OUT.CampaignResponseOut;
import com.example.fproject.Enum.CampaignStatus;
import com.example.fproject.Enum.CampaignType;
import com.example.fproject.Enum.SuggestionStatus;
import com.example.fproject.Model.AIQuestion;
import com.example.fproject.Model.Branch;
import com.example.fproject.Model.Campaign;
import com.example.fproject.Model.CampaignResult;
import com.example.fproject.Model.CampaignSuggestion;
import com.example.fproject.Repository.AiQuestionRepository;
import com.example.fproject.Repository.BranchRepository;
import com.example.fproject.Repository.CampaignResultRepository;
import com.example.fproject.Repository.CampaignRepository;
import com.example.fproject.Repository.CampaignSuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final BranchRepository branchRepository;
    private final CampaignSuggestionRepository campaignSuggestionRepository;
    private final AiQuestionRepository aiQuestionRepository;
    private final CampaignResultRepository campaignResultRepository;
    private final ModelMapper modelMapper;

    public List<CampaignResponseOut> getAllCampaigns() {
        List<CampaignResponseOut> campaigns = new ArrayList<>();
        for (Campaign campaign : campaignRepository.findAll()) {
            campaigns.add(mapCampaign(campaign));
        }
        return campaigns;
    }

    public CampaignResponseOut getCampaignById(Integer campaignId) {
        return mapCampaign(checkCampaign(campaignId));
    }

    public void addCampaign(CampaignRequestIn dto) {
        validateCampaign(dto);
        Campaign campaign = new Campaign();
        setCampaign(campaign, dto);
        campaign.setStatus(CampaignStatus.PENDING);
        campaignRepository.save(campaign);
    }

    public void updateCampaign(Integer campaignId, CampaignRequestIn dto) {
        validateCampaign(dto);
        Campaign old = checkCampaign(campaignId);
        setCampaign(old, dto);
        campaignRepository.save(old);
    }

    public void deleteCampaign(Integer campaignId) {
        // Business note: this CRUD method exists for full coverage, but real workflow may change status instead of hard delete.
        Campaign campaign = checkCampaign(campaignId);
        if (campaign.getCampaignMessages() != null && !campaign.getCampaignMessages().isEmpty()) {
            throw new ApiException("Cannot delete campaign because it has campaign messages");
        }
        if (campaign.getQrCode() != null) {
            throw new ApiException("Cannot delete campaign because it has a QR code");
        }
        if (campaign.getCampaignResult() != null) {
            throw new ApiException("Cannot delete campaign because it has a campaign result");
        }
        campaignRepository.delete(campaign);
    }

    private void setCampaign(Campaign campaign, CampaignRequestIn dto) {
        campaign.setTitle(dto.getTitle());
        campaign.setDescription(dto.getDescription());
        campaign.setOfferText(dto.getOfferText());
        campaign.setCampaignType(dto.getCampaignType());
        campaign.setStartDateTime(dto.getStartDateTime());
        campaign.setEndDateTime(dto.getEndDateTime());
        campaign.setTargetCustomersCount(dto.getTargetCustomersCount());
        campaign.setSentCount(dto.getSentCount());
        campaign.setRedeemedCount(dto.getRedeemedCount());
        Branch branch = checkBranch(dto.getBranchId());
        CampaignSuggestion campaignSuggestion = checkCampaignSuggestion(dto.getCampaignSuggestionId(), campaign.getId());
        validateCampaignSuggestion(campaignSuggestion, branch, dto.getCampaignType());
        campaign.setBranch(branch);
        campaign.setCampaignSuggestion(campaignSuggestion);
        campaign.setAiQuestion(checkAiQuestion(dto.getAiQuestionId(), campaign.getId(), dto.getCampaignType()));
        campaign.setCampaignResult(checkCampaignResult(dto.getCampaignResultId(), campaign.getId()));
    }

    private void validateCampaign(CampaignRequestIn dto) {
        if (!dto.getEndDateTime().isAfter(dto.getStartDateTime())) {
            throw new ApiException("Campaign end time must be after start time");
        }
        if (dto.getSentCount() > dto.getTargetCustomersCount()) {
            throw new ApiException("Sent count cannot be greater than target customers count");
        }
        if (dto.getRedeemedCount() > dto.getSentCount()) {
            throw new ApiException("Redeemed count cannot be greater than sent count");
        }
        if (dto.getRedeemedCount() > dto.getTargetCustomersCount()) {
            throw new ApiException("Redeemed count cannot be greater than target customers count");
        }
        if (dto.getCampaignType() == CampaignType.DIRECT_OFFER && dto.getAiQuestionId() != null) {
            throw new ApiException("Direct offer campaign cannot have an AI question");
        }
    }

    private Campaign checkCampaign(Integer campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException("Campaign not found"));
    }

    private Branch checkBranch(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);
        if (branch == null) {
            throw new ApiException("Branch not found");
        }
        return branch;
    }

    private CampaignSuggestion checkCampaignSuggestion(Integer campaignSuggestionId, Integer campaignId) {
        if (campaignSuggestionId == null) return null;
        CampaignSuggestion campaignSuggestion = campaignSuggestionRepository.findById(campaignSuggestionId)
                .orElseThrow(() -> new ApiException("Campaign suggestion not found"));
        if (campaignSuggestion.getApprovalStatus() != SuggestionStatus.APPROVED) {
            throw new ApiException("Campaign suggestion must be approved before creating a campaign");
        }
        if (campaignSuggestion.getCampaign() != null && !campaignSuggestion.getCampaign().getId().equals(campaignId)) {
            throw new ApiException("Campaign suggestion is already linked to another campaign");
        }
        return campaignSuggestion;
    }

    private void validateCampaignSuggestion(CampaignSuggestion campaignSuggestion, Branch branch, CampaignType campaignType) {
        if (campaignSuggestion == null) return;
        Branch suggestionBranch = campaignSuggestion.getAiAnalysis().getSalesRecord().getBranch();
        if (!suggestionBranch.getId().equals(branch.getId())) {
            throw new ApiException("Campaign suggestion does not belong to this branch");
        }
        if (!campaignSuggestion.getCampaignType().equals(campaignType)) {
            throw new ApiException("Campaign type must match campaign suggestion type");
        }
    }

    private AIQuestion checkAiQuestion(Integer aiQuestionId, Integer campaignId, CampaignType campaignType) {
        if (aiQuestionId == null) return null;
        if (campaignType != CampaignType.QUESTION_BASED) {
            throw new ApiException("AI question can only be linked to question based campaign");
        }
        AIQuestion aiQuestion = aiQuestionRepository.findById(aiQuestionId)
                .orElseThrow(() -> new ApiException("AI question not found"));
        if (aiQuestion.getCampaign() != null && !aiQuestion.getCampaign().getId().equals(campaignId)) {
            throw new ApiException("AI question is already linked to another campaign");
        }
        return aiQuestion;
    }

    private CampaignResult checkCampaignResult(Integer campaignResultId, Integer campaignId) {
        if (campaignResultId == null) return null;
        CampaignResult campaignResult = campaignResultRepository.findById(campaignResultId)
                .orElseThrow(() -> new ApiException("Campaign result not found"));
        if (campaignResult.getCampaign() != null && !campaignResult.getCampaign().getId().equals(campaignId)) {
            throw new ApiException("Campaign result is already linked to another campaign");
        }
        return campaignResult;
    }

    private CampaignResponseOut mapCampaign(Campaign campaign) {
        CampaignResponseOut out = modelMapper.map(campaign, CampaignResponseOut.class);
        out.setBranchId(campaign.getBranch() == null ? null : campaign.getBranch().getId());
        out.setCampaignSuggestionId(campaign.getCampaignSuggestion() == null ? null : campaign.getCampaignSuggestion().getId());
        out.setAiQuestionId(campaign.getAiQuestion() == null ? null : campaign.getAiQuestion().getId());
        out.setQrCodeId(campaign.getQrCode() == null ? null : campaign.getQrCode().getId());
        out.setCampaignResultId(campaign.getCampaignResult() == null ? null : campaign.getCampaignResult().getId());
        return out;
    }
}
