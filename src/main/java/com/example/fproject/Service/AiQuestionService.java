package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.AiQuestionRequestIn;
import com.example.fproject.DTO.OUT.AiQuestionResponseOut;
import com.example.fproject.Enum.CampaignStatus;
import com.example.fproject.Enum.CampaignType;
import com.example.fproject.Model.AIQuestion;
import com.example.fproject.Model.Campaign;
import com.example.fproject.Repository.AiQuestionRepository;
import com.example.fproject.Repository.CampaignRepository;
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
    private final ModelMapper modelMapper;
    private final OpenAiService openAiService;

    public List<AiQuestionResponseOut> getAllAiQuestions() {
        List<AiQuestionResponseOut> aiQuestions = new ArrayList<>();
        for (AIQuestion aiQuestion : aiQuestionRepository.findAll()) {
            aiQuestions.add(mapAiQuestion(aiQuestion));
        }
        return aiQuestions;
    }

    public AiQuestionResponseOut getAiQuestionById(Integer aiQuestionId) {
        return mapAiQuestion(checkAiQuestion(aiQuestionId));
    }

    public AiQuestionResponseOut generateAiQuestion() {
        OpenAiService.AiQuestionResult result = openAiService.generateAiQuestion();

        if (!isAnswerOption(result.correctOption())) {
            throw new ApiException("Correct option must be A, B, or C");
        }

        if (result.optionA().equalsIgnoreCase(result.optionB())
                || result.optionA().equalsIgnoreCase(result.optionC())
                || result.optionB().equalsIgnoreCase(result.optionC())) {
            throw new ApiException("AI question options must be different");
        }

        return new AiQuestionResponseOut(
                null,
                result.questionText(),
                result.optionA(),
                result.optionB(),
                result.optionC(),
                result.correctOption(),
                null
        );
    }

    @Transactional
    public AiQuestionResponseOut generateAiQuestionForCampaign(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        validateCampaignCanGenerateQuestion(campaign);
        if (campaign.getAiQuestion() != null) {
            throw new ApiException("Campaign already has an AI question");
        }
        AIQuestion aiQuestion = buildGeneratedQuestion(campaign);
        AIQuestion saved = aiQuestionRepository.save(aiQuestion);
        campaign.setAiQuestion(saved);
        campaignRepository.save(campaign);
        return mapAiQuestion(saved);
    }

    @Transactional
    public AiQuestionResponseOut regenerateAiQuestion(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        validateCampaignCanGenerateQuestion(campaign);
        AIQuestion aiQuestion = campaign.getAiQuestion();
        if (aiQuestion == null) {
            aiQuestion = new AIQuestion();
            aiQuestion.setCampaign(campaign);
        }
        OpenAiService.AiQuestionResult result = getValidAiQuestionResult();
        setGeneratedQuestion(aiQuestion, result);
        AIQuestion saved = aiQuestionRepository.save(aiQuestion);
        campaign.setAiQuestion(saved);
        campaignRepository.save(campaign);
        return mapAiQuestion(saved);
    }

    public AiQuestionResponseOut getAiQuestionByCampaignId(Integer campaignId) {
        checkCampaign(campaignId);
        AIQuestion aiQuestion = aiQuestionRepository.findAIQuestionByCampaignId(campaignId);
        if (aiQuestion == null) {
            throw new ApiException("Campaign AI question not found");
        }
        return mapAiQuestion(aiQuestion);
    }

    public void addAiQuestion(AiQuestionRequestIn dto) {
        validateAiQuestion(dto);
        AIQuestion aiQuestion = new AIQuestion();
        setAiQuestion(aiQuestion, dto);
        AIQuestion saved = aiQuestionRepository.save(aiQuestion);
        linkCampaign(saved, dto.getCampaignId());
    }

    public void updateAiQuestion(Integer aiQuestionId, AiQuestionRequestIn dto) {
        validateAiQuestion(dto);
        AIQuestion old = checkAiQuestion(aiQuestionId);
        setAiQuestion(old, dto);
        AIQuestion saved = aiQuestionRepository.save(old);
        linkCampaign(saved, dto.getCampaignId());
    }

    public void deleteAiQuestion(Integer aiQuestionId) {
        // Business note: this CRUD method exists for full coverage, but real workflow may detach or update the question instead of hard delete.
        aiQuestionRepository.delete(checkAiQuestion(aiQuestionId));
    }

    private void setAiQuestion(AIQuestion aiQuestion, AiQuestionRequestIn dto) {
        aiQuestion.setQuestionText(dto.getQuestionText());
        aiQuestion.setOptionA(dto.getOptionA());
        aiQuestion.setOptionB(dto.getOptionB());
        aiQuestion.setOptionC(dto.getOptionC());
        aiQuestion.setCorrectOption(dto.getCorrectOption());
    }

    private AIQuestion buildGeneratedQuestion(Campaign campaign) {
        OpenAiService.AiQuestionResult result = getValidAiQuestionResult();
        AIQuestion aiQuestion = new AIQuestion();
        setGeneratedQuestion(aiQuestion, result);
        aiQuestion.setCampaign(campaign);
        return aiQuestion;
    }

    private void setGeneratedQuestion(AIQuestion aiQuestion, OpenAiService.AiQuestionResult result) {
        aiQuestion.setQuestionText(result.questionText());
        aiQuestion.setOptionA(result.optionA());
        aiQuestion.setOptionB(result.optionB());
        aiQuestion.setOptionC(result.optionC());
        aiQuestion.setCorrectOption(result.correctOption());
    }

    private OpenAiService.AiQuestionResult getValidAiQuestionResult() {
        OpenAiService.AiQuestionResult result = openAiService.generateAiQuestion();
        if (!isAnswerOption(result.correctOption())) {
            throw new ApiException("Correct option must be A, B, or C");
        }
        if (result.optionA().equalsIgnoreCase(result.optionB())
                || result.optionA().equalsIgnoreCase(result.optionC())
                || result.optionB().equalsIgnoreCase(result.optionC())) {
            throw new ApiException("AI question options must be different");
        }
        return result;
    }

    private void linkCampaign(AIQuestion aiQuestion, Integer campaignId) {
        if (campaignId == null) return;
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException("Campaign not found"));
        if (campaign.getCampaignType() != CampaignType.QUESTION_BASED) {
            throw new ApiException("AI question can only be linked to question based campaign");
        }
        if (campaign.getAiQuestion() != null && !campaign.getAiQuestion().getId().equals(aiQuestion.getId())) {
            throw new ApiException("Campaign already has an AI question");
        }
        campaign.setAiQuestion(aiQuestion);
        campaignRepository.save(campaign);
    }

    private void validateAiQuestion(AiQuestionRequestIn dto) {
        if (!isAnswerOption(dto.getCorrectOption())) {
            throw new ApiException("Correct option must be A, B, or C");
        }
        if (dto.getOptionA().equalsIgnoreCase(dto.getOptionB())
                || dto.getOptionA().equalsIgnoreCase(dto.getOptionC())
                || dto.getOptionB().equalsIgnoreCase(dto.getOptionC())) {
            throw new ApiException("AI question options must be different");
        }
    }

    private void validateCampaignCanGenerateQuestion(Campaign campaign) {
        if (campaign.getCampaignType() != CampaignType.QUESTION_BASED) {
            throw new ApiException("AI question can only be generated for question based campaign");
        }
        if (campaign.getStatus() == CampaignStatus.ACTIVE || campaign.getStatus() == CampaignStatus.COMPLETED
                || campaign.getStatus() == CampaignStatus.EXPIRED || campaign.getStatus() == CampaignStatus.STOPPED
                || campaign.getStatus() == CampaignStatus.CANCELED) {
            throw new ApiException("Cannot generate AI question after campaign starts or ends");
        }
    }

    private boolean isAnswerOption(String option) {
        return option != null && (option.equals("A") || option.equals("B") || option.equals("C"));
    }

    private AIQuestion checkAiQuestion(Integer aiQuestionId) {
        return aiQuestionRepository.findById(aiQuestionId)
                .orElseThrow(() -> new ApiException("AI question not found"));
    }

    private Campaign checkCampaign(Integer campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException("Campaign not found"));
    }

    private AiQuestionResponseOut mapAiQuestion(AIQuestion aiQuestion) {
        AiQuestionResponseOut out = modelMapper.map(aiQuestion, AiQuestionResponseOut.class);
        out.setCampaignId(aiQuestion.getCampaign() == null ? null : aiQuestion.getCampaign().getId());
        return out;
    }
}
