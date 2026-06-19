package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.CustomerAnswerRequestIn;
import com.example.fproject.DTO.OUT.CustomerAnswerResponseOut;
import com.example.fproject.Enum.CampaignStatus;
import com.example.fproject.Enum.CampaignType;
import com.example.fproject.Model.Campaign;
import com.example.fproject.Model.CampaignMessage;
import com.example.fproject.Model.Customer;
import com.example.fproject.Model.CustomerAnswer;
import com.example.fproject.Repository.CampaignMessageRepository;
import com.example.fproject.Repository.CampaignRepository;
import com.example.fproject.Repository.CustomerAnswerRepository;
import com.example.fproject.Repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerAnswerService {

    private final CustomerAnswerRepository customerAnswerRepository;
    private final CampaignRepository campaignRepository;
    private final CustomerRepository customerRepository;
    private final CampaignMessageRepository campaignMessageRepository;
    private final ModelMapper modelMapper;

    public List<CustomerAnswerResponseOut> getAllCustomerAnswers() {
        List<CustomerAnswerResponseOut> answers = new ArrayList<>();
        for (CustomerAnswer answer : customerAnswerRepository.findAll()) {
            answers.add(mapCustomerAnswer(answer));
        }
        return answers;
    }

    public CustomerAnswerResponseOut getCustomerAnswerById(Integer customerAnswerId) {
        return mapCustomerAnswer(checkCustomerAnswer(customerAnswerId));
    }

    @Transactional
    public CustomerAnswerResponseOut answerCampaignMessage(Integer campaignMessageId, String answer) {
        CampaignMessage message = campaignMessageRepository.findById(campaignMessageId)
                .orElseThrow(() -> new ApiException("Campaign message not found"));
        Campaign campaign = message.getCampaign();
        Customer customer = message.getCustomer();
        String selectedOption = normalizeAnswer(answer);

        if (message.getCustomerAnswer() != null) {
            throw new ApiException("Customer already answered this campaign message");
        }
        if (customerAnswerRepository.existsByCampaignIdAndCustomerId(campaign.getId(), customer.getId())) {
            throw new ApiException("Customer already answered this campaign");
        }
        if (campaign.getCampaignType() != CampaignType.QUESTION_BASED) {
            throw new ApiException("Customer answer can only be added to question based campaign");
        }
        if (campaign.getAiQuestion() == null) {
            throw new ApiException("Campaign does not have an AI question");
        }
        validateCampaignActiveNow(campaign);

        CustomerAnswer customerAnswer = new CustomerAnswer();
        customerAnswer.setSelectedOption(selectedOption);
        customerAnswer.setCorrect(campaign.getAiQuestion().getCorrectOption().equals(selectedOption));
        customerAnswer.setAttemptedAt(LocalDateTime.now());
        customerAnswer.setCustomer(customer);
        customerAnswer.setCampaign(campaign);
        CustomerAnswer saved = customerAnswerRepository.save(customerAnswer);
        message.setCustomerAnswer(saved);
        campaignMessageRepository.save(message);
        return mapCustomerAnswer(saved);
    }

    public CustomerAnswerResponseOut getCustomerAnswerByCampaignMessage(Integer campaignMessageId) {
        CampaignMessage message = campaignMessageRepository.findById(campaignMessageId)
                .orElseThrow(() -> new ApiException("Campaign message not found"));
        CustomerAnswer answer = customerAnswerRepository.findCustomerAnswerByCampaignMessageId(message.getId());
        if (answer == null) {
            throw new ApiException("Customer answer not found");
        }
        return mapCustomerAnswer(answer);
    }

    public List<CustomerAnswerResponseOut> getAnswersByCampaign(Integer campaignId) {
        checkCampaign(campaignId);
        List<CustomerAnswerResponseOut> answers = new ArrayList<>();
        for (CustomerAnswer answer : customerAnswerRepository.findAllByCampaignId(campaignId)) {
            answers.add(mapCustomerAnswer(answer));
        }
        return answers;
    }

    @Transactional
    public void addCustomerAnswer(CustomerAnswerRequestIn dto) {
        validateDuplicateCustomerAnswer(dto, null);
        CustomerAnswer customerAnswer = new CustomerAnswer();
        setCustomerAnswer(customerAnswer, dto);
        CustomerAnswer saved = customerAnswerRepository.save(customerAnswer);
        linkCampaignMessage(saved, dto.getCampaignMessageId());
    }

    @Transactional
    public void updateCustomerAnswer(Integer customerAnswerId, CustomerAnswerRequestIn dto) {
        CustomerAnswer old = checkCustomerAnswer(customerAnswerId);
        validateDuplicateCustomerAnswer(dto, customerAnswerId);
        setCustomerAnswer(old, dto);
        CustomerAnswer saved = customerAnswerRepository.save(old);
        linkCampaignMessage(saved, dto.getCampaignMessageId());
    }

    public void deleteCustomerAnswer(Integer customerAnswerId) {
        // Business note: this CRUD method exists for full coverage, but real workflow may keep customer answers for campaign evaluation.
        customerAnswerRepository.delete(checkCustomerAnswer(customerAnswerId));
    }

    private void setCustomerAnswer(CustomerAnswer customerAnswer, CustomerAnswerRequestIn dto) {
        Campaign campaign = checkCampaign(dto.getCampaignId());
        Customer customer = checkCustomer(dto.getCustomerId());
        validateCustomerAnswer(dto, campaign);
        customerAnswer.setSelectedOption(dto.getSelectedOption());
        customerAnswer.setCorrect(campaign.getAiQuestion().getCorrectOption().equals(dto.getSelectedOption()));
        customerAnswer.setAttemptedAt(dto.getAttemptedAt());
        customerAnswer.setCustomer(customer);
        customerAnswer.setCampaign(campaign);
    }

    private void validateDuplicateCustomerAnswer(CustomerAnswerRequestIn dto, Integer customerAnswerId) {
        Boolean exists = customerAnswerRepository.existsByCampaignIdAndCustomerId(dto.getCampaignId(), dto.getCustomerId());
        if (Boolean.TRUE.equals(exists) && customerAnswerId == null) {
            throw new ApiException("Customer already answered this campaign");
        }
    }

    private void linkCampaignMessage(CustomerAnswer customerAnswer, Integer campaignMessageId) {
        if (campaignMessageId == null) return;
        CampaignMessage message = campaignMessageRepository.findById(campaignMessageId)
                .orElseThrow(() -> new ApiException("Campaign message not found"));
        if (!message.getCampaign().getId().equals(customerAnswer.getCampaign().getId())
                || !message.getCustomer().getId().equals(customerAnswer.getCustomer().getId())) {
            throw new ApiException("Campaign message does not belong to this customer answer");
        }
        message.setCustomerAnswer(customerAnswer);
        campaignMessageRepository.save(message);
    }

    private void validateCustomerAnswer(CustomerAnswerRequestIn dto, Campaign campaign) {
        if (!isAnswerOption(dto.getSelectedOption())) {
            throw new ApiException("Selected option must be A, B, or C");
        }
        if (campaign.getCampaignType() != CampaignType.QUESTION_BASED) {
            throw new ApiException("Customer answer can only be added to question based campaign");
        }
        if (campaign.getAiQuestion() == null) {
            throw new ApiException("Campaign does not have an AI question");
        }
        validateCampaignActiveNow(campaign);
    }

    private void validateCampaignActiveNow(Campaign campaign) {
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new ApiException("Campaign must be active before accepting answers");
        }
        LocalDateTime now = LocalDateTime.now();
        if (campaign.getStartDateTime() == null || campaign.getEndDateTime() == null) {
            throw new ApiException("Campaign time is required");
        }
        if (now.isBefore(campaign.getStartDateTime()) || now.isAfter(campaign.getEndDateTime())) {
            throw new ApiException("Campaign is outside its active time");
        }
    }

    private boolean isAnswerOption(String option) {
        return option != null && (option.equals("A") || option.equals("B") || option.equals("C"));
    }

    private String normalizeAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            throw new ApiException("Answer is required");
        }
        String selectedOption = answer.trim().toUpperCase();
        if (!isAnswerOption(selectedOption)) {
            throw new ApiException("Selected option must be A, B, or C");
        }
        return selectedOption;
    }

    private CustomerAnswer checkCustomerAnswer(Integer customerAnswerId) {
        return customerAnswerRepository.findById(customerAnswerId)
                .orElseThrow(() -> new ApiException("Customer answer not found"));
    }

    private Customer checkCustomer(Integer customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ApiException("Customer not found"));
    }

    private Campaign checkCampaign(Integer campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException("Campaign not found"));
    }

    private CustomerAnswerResponseOut mapCustomerAnswer(CustomerAnswer customerAnswer) {
        CustomerAnswerResponseOut out = modelMapper.map(customerAnswer, CustomerAnswerResponseOut.class);
        out.setCustomerId(customerAnswer.getCustomer() == null ? null : customerAnswer.getCustomer().getId());
        out.setCampaignId(customerAnswer.getCampaign() == null ? null : customerAnswer.getCampaign().getId());
        out.setCampaignMessageId(customerAnswer.getCampaignMessage() == null ? null : customerAnswer.getCampaignMessage().getId());
        return out;
    }
}
