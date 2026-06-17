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
        CustomerAnswer customerAnswer = new CustomerAnswer();
        setCustomerAnswer(customerAnswer, dto);
        CustomerAnswer saved = customerAnswerRepository.save(customerAnswer);
        linkCampaignMessage(saved, dto.getCampaignMessageId());
    }

    public void updateCustomerAnswer(Integer customerAnswerId, CustomerAnswerRequestIn dto) {
        CustomerAnswer old = checkCustomerAnswer(customerAnswerId);
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
        validateCustomerAnswer(dto, campaign);
        customerAnswer.setSelectedOption(dto.getSelectedOption());
        customerAnswer.setCorrect(dto.getCorrect());
        customerAnswer.setAttemptedAt(dto.getAttemptedAt());
        customerAnswer.setCustomer(checkCustomer(dto.getCustomerId()));
        customerAnswer.setCampaign(campaign);
    }

    private void linkCampaignMessage(CustomerAnswer customerAnswer, Integer campaignMessageId) {
        if (campaignMessageId == null) return;
        CampaignMessage message = campaignMessageRepository.findById(campaignMessageId)
                .orElseThrow(() -> new ApiException("Campaign message not found"));
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
