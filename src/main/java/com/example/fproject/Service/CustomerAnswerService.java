package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.CustomerAnswerRequestIn;
import com.example.fproject.DTO.OUT.CustomerAnswerResponseOut;
import com.example.fproject.Enum.CampaignStatus;
import com.example.fproject.Enum.CampaignType;
import com.example.fproject.Model.*;
import com.example.fproject.Repository.*;
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
    private final StoreOwnerRepository storeOwnerRepository;
    private final ModelMapper modelMapper;

    public List<CustomerAnswerResponseOut> getAllCustomerAnswers() {
        List<CustomerAnswerResponseOut> result = new ArrayList<>();
        for (CustomerAnswer a : customerAnswerRepository.findAll()) result.add(mapCustomerAnswer(a));
        return result;
    }

    // STORE_OWNER
    public CustomerAnswerResponseOut getCustomerAnswerById(Integer userId, Integer customerAnswerId) {
        CustomerAnswer a = checkCustomerAnswer(customerAnswerId);
        verifyStoreOwnerOwnership(userId, a.getCampaign());
        return mapCustomerAnswer(a);
    }

    // STORE_OWNER
    public CustomerAnswerResponseOut getCustomerAnswerByCampaignMessage(Integer userId, Integer campaignMessageId) {
        CampaignMessage message = campaignMessageRepository.findById(campaignMessageId)
                .orElseThrow(() -> new ApiException("Campaign message not found"));
        verifyStoreOwnerOwnership(userId, message.getCampaign());
        CustomerAnswer a = customerAnswerRepository.findCustomerAnswerByCampaignMessageId(message.getId());
        if (a == null) throw new ApiException("Customer answer not found");
        return mapCustomerAnswer(a);
    }

    // STORE_OWNER
    public List<CustomerAnswerResponseOut> getAnswersByCampaign(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyStoreOwnerOwnership(userId, campaign);
        List<CustomerAnswerResponseOut> result = new ArrayList<>();
        for (CustomerAnswer a : customerAnswerRepository.findAllByCampaignId(campaignId)) result.add(mapCustomerAnswer(a));
        return result;
    }

    // CUSTOMER — يجاوب على رسالة حملة بالـ userId من الـ token
    @Transactional
    public CustomerAnswerResponseOut answerCampaignMessage(Integer userId, Integer campaignMessageId, String answer) {
        CampaignMessage message = campaignMessageRepository.findById(campaignMessageId)
                .orElseThrow(() -> new ApiException("Campaign message not found"));

        // تحقق إن الرسالة تخص هذا الـ customer
        if (message.getCustomer() == null || !message.getCustomer().getId().equals(userId))
            throw new ApiException("This campaign message does not belong to you");

        Campaign campaign = message.getCampaign();
        Customer customer = message.getCustomer();
        String selectedOption = normalizeAnswer(answer);

        if (message.getCustomerAnswer() != null) throw new ApiException("Customer already answered this campaign message");
        if (customerAnswerRepository.existsByCampaignIdAndCustomerId(campaign.getId(), customer.getId()))
            throw new ApiException("Customer already answered this campaign");
        if (campaign.getCampaignType() != CampaignType.QUESTION_BASED)
            throw new ApiException("Customer answer can only be added to question based campaign");
        if (campaign.getAiQuestion() == null) throw new ApiException("Campaign does not have an AI question");
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

    // STORE_OWNER
    @Transactional
    public void addCustomerAnswer(Integer userId, CustomerAnswerRequestIn dto) {
        Campaign campaign = checkCampaign(dto.getCampaignId());
        verifyStoreOwnerOwnership(userId, campaign);
        validateDuplicateCustomerAnswer(dto, null);
        CustomerAnswer a = new CustomerAnswer();
        setCustomerAnswer(a, dto);
        CustomerAnswer saved = customerAnswerRepository.save(a);
        linkCampaignMessage(saved, dto.getCampaignMessageId());
    }

    // STORE_OWNER
    @Transactional
    public void updateCustomerAnswer(Integer userId, Integer customerAnswerId, CustomerAnswerRequestIn dto) {
        CustomerAnswer old = checkCustomerAnswer(customerAnswerId);
        verifyStoreOwnerOwnership(userId, old.getCampaign());
        validateDuplicateCustomerAnswer(dto, customerAnswerId);
        setCustomerAnswer(old, dto);
        CustomerAnswer saved = customerAnswerRepository.save(old);
        linkCampaignMessage(saved, dto.getCampaignMessageId());
    }

    // STORE_OWNER
    public void deleteCustomerAnswer(Integer userId, Integer customerAnswerId) {
        CustomerAnswer a = checkCustomerAnswer(customerAnswerId);
        verifyStoreOwnerOwnership(userId, a.getCampaign());
        customerAnswerRepository.delete(a);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void verifyStoreOwnerOwnership(Integer userId, Campaign campaign) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null || campaign.getBranch() == null
                || !campaign.getBranch().getStore().getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("You do not have permission to access this resource");
    }

    private void setCustomerAnswer(CustomerAnswer a, CustomerAnswerRequestIn dto) {
        Campaign campaign = checkCampaign(dto.getCampaignId());
        Customer customer = checkCustomer(dto.getCustomerId());
        validateCustomerAnswer(dto, campaign);
        a.setSelectedOption(dto.getSelectedOption());
        a.setCorrect(campaign.getAiQuestion().getCorrectOption().equals(dto.getSelectedOption()));
        a.setAttemptedAt(dto.getAttemptedAt());
        a.setCustomer(customer);
        a.setCampaign(campaign);
    }

    private void validateDuplicateCustomerAnswer(CustomerAnswerRequestIn dto, Integer customerAnswerId) {
        if (Boolean.TRUE.equals(customerAnswerRepository.existsByCampaignIdAndCustomerId(
                dto.getCampaignId(), dto.getCustomerId())) && customerAnswerId == null)
            throw new ApiException("Customer already answered this campaign");
    }

    private void linkCampaignMessage(CustomerAnswer a, Integer campaignMessageId) {
        if (campaignMessageId == null) return;
        CampaignMessage message = campaignMessageRepository.findById(campaignMessageId)
                .orElseThrow(() -> new ApiException("Campaign message not found"));
        if (!message.getCampaign().getId().equals(a.getCampaign().getId())
                || !message.getCustomer().getId().equals(a.getCustomer().getId()))
            throw new ApiException("Campaign message does not belong to this customer answer");
        message.setCustomerAnswer(a);
        campaignMessageRepository.save(message);
    }

    private void validateCustomerAnswer(CustomerAnswerRequestIn dto, Campaign campaign) {
        if (!isAnswerOption(dto.getSelectedOption())) throw new ApiException("Selected option must be A, B, or C");
        if (campaign.getCampaignType() != CampaignType.QUESTION_BASED)
            throw new ApiException("Customer answer can only be added to question based campaign");
        if (campaign.getAiQuestion() == null) throw new ApiException("Campaign does not have an AI question");
        validateCampaignActiveNow(campaign);
    }

    private void validateCampaignActiveNow(Campaign campaign) {
        if (campaign.getStatus() != CampaignStatus.ACTIVE) throw new ApiException("Campaign must be active before accepting answers");
        LocalDateTime now = LocalDateTime.now();
        if (campaign.getStartDateTime() == null || campaign.getEndDateTime() == null)
            throw new ApiException("Campaign time is required");
        if (now.isBefore(campaign.getStartDateTime()) || now.isAfter(campaign.getEndDateTime()))
            throw new ApiException("Campaign is outside its active time");
    }

    private boolean isAnswerOption(String option) {
        return option != null && (option.equals("A") || option.equals("B") || option.equals("C"));
    }

    private String normalizeAnswer(String answer) {
        if (answer == null || answer.isBlank()) throw new ApiException("Answer is required");
        String opt = answer.trim().toUpperCase();
        if (!isAnswerOption(opt)) throw new ApiException("Selected option must be A, B, or C");
        return opt;
    }

    private CustomerAnswer checkCustomerAnswer(Integer id) {
        return customerAnswerRepository.findById(id).orElseThrow(() -> new ApiException("Customer answer not found"));
    }

    private Customer checkCustomer(Integer id) {
        return customerRepository.findById(id).orElseThrow(() -> new ApiException("Customer not found"));
    }

    private Campaign checkCampaign(Integer id) {
        return campaignRepository.findById(id).orElseThrow(() -> new ApiException("Campaign not found"));
    }

    private CustomerAnswerResponseOut mapCustomerAnswer(CustomerAnswer a) {
        CustomerAnswerResponseOut out = modelMapper.map(a, CustomerAnswerResponseOut.class);
        out.setCustomerId(a.getCustomer() == null ? null : a.getCustomer().getId());
        out.setCampaignId(a.getCampaign() == null ? null : a.getCampaign().getId());
        out.setCampaignMessageId(a.getCampaignMessage() == null ? null : a.getCampaignMessage().getId());
        return out;
    }
}