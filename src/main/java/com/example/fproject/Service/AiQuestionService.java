package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.AiQuestionRequestIn;
import com.example.fproject.DTO.OUT.AiQuestionResponseOut;
import com.example.fproject.Enum.CampaignType;
import com.example.fproject.Model.AIQuestion;
import com.example.fproject.Model.Campaign;
import com.example.fproject.Repository.AiQuestionRepository;
import com.example.fproject.Repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiQuestionService {

    private final AiQuestionRepository aiQuestionRepository;
    private final CampaignRepository campaignRepository;
    private final ModelMapper modelMapper;

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

    private boolean isAnswerOption(String option) {
        return option != null && (option.equals("A") || option.equals("B") || option.equals("C"));
    }

    private AIQuestion checkAiQuestion(Integer aiQuestionId) {
        return aiQuestionRepository.findById(aiQuestionId)
                .orElseThrow(() -> new ApiException("AI question not found"));
    }

    private AiQuestionResponseOut mapAiQuestion(AIQuestion aiQuestion) {
        AiQuestionResponseOut out = modelMapper.map(aiQuestion, AiQuestionResponseOut.class);
        out.setCampaignId(aiQuestion.getCampaign() == null ? null : aiQuestion.getCampaign().getId());
        return out;
    }
}
