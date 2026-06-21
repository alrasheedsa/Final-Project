package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.AiQuestionRequestIn;
import com.example.fproject.DTO.OUT.AiQuestionResponseOut;
import com.example.fproject.Enum.CampaignStatus;
import com.example.fproject.Enum.CampaignType;
import com.example.fproject.Enum.RoleType;
import com.example.fproject.Model.AIQuestion;
import com.example.fproject.Model.Campaign;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Model.User;
import com.example.fproject.Repository.AiQuestionRepository;
import com.example.fproject.Repository.CampaignRepository;
import com.example.fproject.Repository.StoreOwnerRepository;
import com.example.fproject.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiQuestionService {

    private final AiQuestionRepository aiQuestionRepository;
    private final CampaignRepository campaignRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final ModelMapper modelMapper;
    private final OpenAiService openAiService;
    private final UserRepository userRepository;

    public List<AiQuestionResponseOut> getAllAiQuestions() {
        List<AiQuestionResponseOut> result = new ArrayList<>();
        for (AIQuestion q : aiQuestionRepository.findAll()) result.add(mapAiQuestion(q));
        return result;
    }

    public AiQuestionResponseOut getAiQuestionById(Integer userId, Integer aiQuestionId) {
        AIQuestion q = checkAiQuestion(aiQuestionId);
        verifyOwnership(userId, q);
        return mapAiQuestion(q);
    }

    public AiQuestionResponseOut generateAiQuestion(Integer userId) {
        // استدعاء عام — التحقق من الـ userId يكفي إنه STORE_OWNER (من الـ Security)
        User user = userRepository.findUserById(userId);
        if (!user.getRole().equals(RoleType.STORE_OWNER)){
            throw new ApiException("you are not allowed to access endpoint");
        }
        OpenAiService.AiQuestionResult result = openAiService.generateAiQuestion();
        if (!isAnswerOption(result.correctOption()))
            throw new ApiException("Correct option must be A, B, or C");
        if (result.optionA().equalsIgnoreCase(result.optionB())
                || result.optionA().equalsIgnoreCase(result.optionC())
                || result.optionB().equalsIgnoreCase(result.optionC()))
            throw new ApiException("AI question options must be different");
        return new AiQuestionResponseOut(null, result.questionText(),
                result.optionA(), result.optionB(), result.optionC(), result.correctOption(), null);
    }

    @Transactional
    public AiQuestionResponseOut generateAiQuestionForCampaign(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnershipByCampaign(userId, campaign);
        validateCampaignCanGenerateQuestion(campaign);
        if (campaign.getAiQuestion() != null) throw new ApiException("Campaign already has an AI question");
        AIQuestion q = buildGeneratedQuestion(campaign);
        AIQuestion saved = aiQuestionRepository.save(q);
        campaign.setAiQuestion(saved);
        campaignRepository.save(campaign);
        return mapAiQuestion(saved);
    }

    @Transactional
    public AiQuestionResponseOut regenerateAiQuestion(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnershipByCampaign(userId, campaign);
        validateCampaignCanGenerateQuestion(campaign);
        AIQuestion q = campaign.getAiQuestion();
        if (q == null) { q = new AIQuestion(); q.setCampaign(campaign); }
        setGeneratedQuestion(q, getValidAiQuestionResult());
        AIQuestion saved = aiQuestionRepository.save(q);
        campaign.setAiQuestion(saved);
        campaignRepository.save(campaign);
        return mapAiQuestion(saved);
    }

    public AiQuestionResponseOut getAiQuestionByCampaignId(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnershipByCampaign(userId, campaign);
        AIQuestion q = aiQuestionRepository.findAIQuestionByCampaignId(campaignId);
        if (q == null) throw new ApiException("Campaign AI question not found");
        return mapAiQuestion(q);
    }

    public void addAiQuestion(Integer userId, AiQuestionRequestIn dto) {
        validateAiQuestion(dto);
        if (dto.getCampaignId() != null) {
            Campaign campaign = checkCampaign(dto.getCampaignId());
            verifyOwnershipByCampaign(userId, campaign);
        }
        AIQuestion q = new AIQuestion();
        setAiQuestion(q, dto);
        AIQuestion saved = aiQuestionRepository.save(q);
        linkCampaign(userId, saved, dto.getCampaignId());
    }

    public void updateAiQuestion(Integer userId, Integer aiQuestionId, AiQuestionRequestIn dto) {
        validateAiQuestion(dto);
        AIQuestion old = checkAiQuestion(aiQuestionId);
        verifyOwnership(userId, old);
        setAiQuestion(old, dto);
        AIQuestion saved = aiQuestionRepository.save(old);
        linkCampaign(userId, saved, dto.getCampaignId());
    }

    public void deleteAiQuestion(Integer userId, Integer aiQuestionId) {
        AIQuestion q = checkAiQuestion(aiQuestionId);
        verifyOwnership(userId, q);
        aiQuestionRepository.delete(q);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void verifyOwnership(Integer userId, AIQuestion aiQuestion) {
        if (aiQuestion.getCampaign() == null) return; // سؤال غير مرتبط بحملة
        verifyOwnershipByCampaign(userId, aiQuestion.getCampaign());
    }

    private void verifyOwnershipByCampaign(Integer userId, Campaign campaign) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null || campaign.getBranch() == null
                || !campaign.getBranch().getStore().getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("You do not have permission to access this resource");
    }

    private void setAiQuestion(AIQuestion q, AiQuestionRequestIn dto) {
        q.setQuestionText(dto.getQuestionText());
        q.setOptionA(dto.getOptionA());
        q.setOptionB(dto.getOptionB());
        q.setOptionC(dto.getOptionC());
        q.setCorrectOption(dto.getCorrectOption());
    }

    private AIQuestion buildGeneratedQuestion(Campaign campaign) {
        AIQuestion q = new AIQuestion();
        setGeneratedQuestion(q, getValidAiQuestionResult());
        q.setCampaign(campaign);
        return q;
    }

    private void setGeneratedQuestion(AIQuestion q, OpenAiService.AiQuestionResult result) {
        q.setQuestionText(result.questionText());
        q.setOptionA(result.optionA());
        q.setOptionB(result.optionB());
        q.setOptionC(result.optionC());
        q.setCorrectOption(result.correctOption());
    }

    private OpenAiService.AiQuestionResult getValidAiQuestionResult() {
        OpenAiService.AiQuestionResult result = openAiService.generateAiQuestion();
        if (!isAnswerOption(result.correctOption())) throw new ApiException("Correct option must be A, B, or C");
        if (result.optionA().equalsIgnoreCase(result.optionB())
                || result.optionA().equalsIgnoreCase(result.optionC())
                || result.optionB().equalsIgnoreCase(result.optionC()))
            throw new ApiException("AI question options must be different");
        return result;
    }

    private void linkCampaign(Integer userId, AIQuestion aiQuestion, Integer campaignId) {
        if (campaignId == null) return;
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException("Campaign not found"));
        verifyOwnershipByCampaign(userId, campaign);
        if (campaign.getCampaignType() != CampaignType.QUESTION_BASED)
            throw new ApiException("AI question can only be linked to question based campaign");
        if (campaign.getAiQuestion() != null && !campaign.getAiQuestion().getId().equals(aiQuestion.getId()))
            throw new ApiException("Campaign already has an AI question");
        campaign.setAiQuestion(aiQuestion);
        campaignRepository.save(campaign);
    }

    private void validateAiQuestion(AiQuestionRequestIn dto) {
        if (!isAnswerOption(dto.getCorrectOption())) throw new ApiException("Correct option must be A, B, or C");
        if (dto.getOptionA().equalsIgnoreCase(dto.getOptionB())
                || dto.getOptionA().equalsIgnoreCase(dto.getOptionC())
                || dto.getOptionB().equalsIgnoreCase(dto.getOptionC()))
            throw new ApiException("AI question options must be different");
    }

    private void validateCampaignCanGenerateQuestion(Campaign campaign) {
        if (campaign.getCampaignType() != CampaignType.QUESTION_BASED)
            throw new ApiException("AI question can only be generated for question based campaign");
        if (campaign.getStatus() == CampaignStatus.ACTIVE || campaign.getStatus() == CampaignStatus.COMPLETED
                || campaign.getStatus() == CampaignStatus.EXPIRED || campaign.getStatus() == CampaignStatus.STOPPED
                || campaign.getStatus() == CampaignStatus.CANCELED)
            throw new ApiException("Cannot generate AI question after campaign starts or ends");
    }

    private boolean isAnswerOption(String option) {
        return option != null && (option.equals("A") || option.equals("B") || option.equals("C"));
    }

    private AIQuestion checkAiQuestion(Integer id) {
        return aiQuestionRepository.findById(id)
                .orElseThrow(() -> new ApiException("AI question not found"));
    }

    private Campaign checkCampaign(Integer id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ApiException("Campaign not found"));
    }

    private AiQuestionResponseOut mapAiQuestion(AIQuestion q) {
        AiQuestionResponseOut out = modelMapper.map(q, AiQuestionResponseOut.class);
        out.setCampaignId(q.getCampaign() == null ? null : q.getCampaign().getId());
        return out;
    }
}