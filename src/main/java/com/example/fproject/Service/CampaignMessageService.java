package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.CampaignMessageRequestIn;
import com.example.fproject.DTO.OUT.CampaignMessageResponseOut;
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
public class CampaignMessageService {

    private final CampaignMessageRepository campaignMessageRepository;
    private final CampaignRepository campaignRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAnswerRepository customerAnswerRepository;
    private final ModelMapper modelMapper;

    public List<CampaignMessageResponseOut> getAllCampaignMessages() {
        List<CampaignMessageResponseOut> messages = new ArrayList<>();
        for (CampaignMessage message : campaignMessageRepository.findAll()) {
            messages.add(mapCampaignMessage(message));
        }
        return messages;
    }

    public CampaignMessageResponseOut getCampaignMessageById(Integer campaignMessageId) {
        return mapCampaignMessage(checkCampaignMessage(campaignMessageId));
    }

    public void addCampaignMessage(CampaignMessageRequestIn dto) {
        validateCampaignMessage(dto, null);
        CampaignMessage campaignMessage = new CampaignMessage();
        setCampaignMessage(campaignMessage, dto);
        campaignMessageRepository.save(campaignMessage);
    }

    public void updateCampaignMessage(Integer campaignMessageId, CampaignMessageRequestIn dto) {
        CampaignMessage old = checkCampaignMessage(campaignMessageId);
        validateCampaignMessage(dto, campaignMessageId);
        setCampaignMessage(old, dto);
        campaignMessageRepository.save(old);
    }

    public void deleteCampaignMessage(Integer campaignMessageId) {
        // Business note: this CRUD method exists for full coverage, but real workflow may keep sent messages for tracking and reports.
        campaignMessageRepository.delete(checkCampaignMessage(campaignMessageId));
    }

    private void setCampaignMessage(CampaignMessage campaignMessage, CampaignMessageRequestIn dto) {
        Campaign campaign = checkCampaign(dto.getCampaignId());
        Customer customer = checkCustomer(dto.getCustomerId());
        CustomerAnswer customerAnswer = checkCustomerAnswer(dto.getCustomerAnswerId());
        validateLinkedCustomerAnswer(customerAnswer, campaign, customer);
        campaignMessage.setMessageText(dto.getMessageText());
        campaignMessage.setDistanceKm(dto.getDistanceKm());
        campaignMessage.setDurationMinutes(dto.getDurationMinutes());
        campaignMessage.setDistanceText(dto.getDistanceText());
        campaignMessage.setStatus(dto.getStatus());
        campaignMessage.setSentAt(dto.getSentAt());
        campaignMessage.setCampaign(campaign);
        campaignMessage.setCustomer(customer);
        campaignMessage.setCustomerAnswer(customerAnswer);
    }

    private void validateCampaignMessage(CampaignMessageRequestIn dto, Integer campaignMessageId) {
        Boolean exists = campaignMessageRepository.existsByCampaignIdAndCustomerId(dto.getCampaignId(), dto.getCustomerId());
        if (Boolean.TRUE.equals(exists) && campaignMessageId == null) {
            throw new ApiException("Campaign message already exists for this customer");
        }
    }

    private CampaignMessage checkCampaignMessage(Integer campaignMessageId) {
        return campaignMessageRepository.findById(campaignMessageId)
                .orElseThrow(() -> new ApiException("Campaign message not found"));
    }

    private Campaign checkCampaign(Integer campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException("Campaign not found"));
    }

    private Customer checkCustomer(Integer customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ApiException("Customer not found"));
    }

    private CustomerAnswer checkCustomerAnswer(Integer customerAnswerId) {
        if (customerAnswerId == null) return null;
        return customerAnswerRepository.findById(customerAnswerId)
                .orElseThrow(() -> new ApiException("Customer answer not found"));
    }

    private void validateLinkedCustomerAnswer(CustomerAnswer customerAnswer, Campaign campaign, Customer customer) {
        if (customerAnswer == null) return;
        if (!customerAnswer.getCampaign().getId().equals(campaign.getId())
                || !customerAnswer.getCustomer().getId().equals(customer.getId())) {
            throw new ApiException("Customer answer does not belong to this campaign message");
        }
    }

    private CampaignMessageResponseOut mapCampaignMessage(CampaignMessage campaignMessage) {
        CampaignMessageResponseOut out = modelMapper.map(campaignMessage, CampaignMessageResponseOut.class);
        out.setCampaignId(campaignMessage.getCampaign() == null ? null : campaignMessage.getCampaign().getId());
        out.setCustomerId(campaignMessage.getCustomer() == null ? null : campaignMessage.getCustomer().getId());
        out.setCustomerAnswerId(campaignMessage.getCustomerAnswer() == null ? null : campaignMessage.getCustomerAnswer().getId());
        return out;
    }
}
