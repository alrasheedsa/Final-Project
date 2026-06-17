package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.CustomerAnswerRequestIn;
import com.example.fproject.DTO.OUT.CustomerAnswerResponseOut;
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

    public void addCustomerAnswer(CustomerAnswerRequestIn dto) {
        validateDuplicateCustomerAnswer(dto, null);
        CustomerAnswer customerAnswer = new CustomerAnswer();
        setCustomerAnswer(customerAnswer, dto);
        CustomerAnswer saved = customerAnswerRepository.save(customerAnswer);
        linkCampaignMessage(saved, dto.getCampaignMessageId());
    }

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
    }

    private boolean isAnswerOption(String option) {
        return option != null && (option.equals("A") || option.equals("B") || option.equals("C"));
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
