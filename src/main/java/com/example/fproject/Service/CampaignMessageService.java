package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.CampaignMessageRequestIn;
import com.example.fproject.DTO.OUT.CampaignMessageResponseOut;
import com.example.fproject.Enum.MessageStatus;
import com.example.fproject.Model.*;
import com.example.fproject.Repository.*;
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
    private final StoreOwnerRepository storeOwnerRepository;
    private final ModelMapper modelMapper;

    public List<CampaignMessageResponseOut> getAllCampaignMessages() {
        List<CampaignMessageResponseOut> result = new ArrayList<>();
        for (CampaignMessage m : campaignMessageRepository.findAll()) result.add(mapCampaignMessage(m));
        return result;
    }

    public CampaignMessageResponseOut getCampaignMessageById(Integer userId, Integer campaignMessageId) {
        CampaignMessage m = checkCampaignMessage(campaignMessageId);
        verifyOwnership(userId, m.getCampaign());
        return mapCampaignMessage(m);
    }

    public List<CampaignMessageResponseOut> getMessagesByCampaign(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        List<CampaignMessageResponseOut> result = new ArrayList<>();
        for (CampaignMessage m : campaignMessageRepository.findAllByCampaignId(campaignId))
            result.add(mapCampaignMessage(m));
        return result;
    }

    // ADMIN — بدون تحقق ملكية
    public List<CampaignMessageResponseOut> getMessagesByCustomer(Integer customerId) {
        if (!customerRepository.existsById(customerId)) throw new ApiException("Customer not found");
        List<CampaignMessageResponseOut> result = new ArrayList<>();
        for (CampaignMessage m : campaignMessageRepository.findAllByCustomerId(customerId))
            result.add(mapCampaignMessage(m));
        return result;
    }

    public CampaignMessageResponseOut getOpenMessageForCustomerPhone(Integer userId, String phone) {
        Customer customer = checkCustomerByPhone(phone);
        for (CampaignMessage m : campaignMessageRepository
                .findAllByCustomerIdAndStatusOrderBySentAtDesc(customer.getId(), MessageStatus.SENT)) {
            if (m.getCustomerAnswer() == null) {
                verifyOwnership(userId, m.getCampaign());
                return mapCampaignMessage(m);
            }
        }
        throw new ApiException("No open campaign message found for this customer");
    }

    public void addCampaignMessage(Integer userId, CampaignMessageRequestIn dto) {
        Campaign campaign = checkCampaign(dto.getCampaignId());
        verifyOwnership(userId, campaign);
        validateCampaignMessage(dto, null);
        CampaignMessage m = new CampaignMessage();
        setCampaignMessage(m, dto);
        campaignMessageRepository.save(m);
    }

    public void updateCampaignMessage(Integer userId, Integer campaignMessageId, CampaignMessageRequestIn dto) {
        CampaignMessage old = checkCampaignMessage(campaignMessageId);
        verifyOwnership(userId, old.getCampaign());
        validateCampaignMessage(dto, campaignMessageId);
        setCampaignMessage(old, dto);
        campaignMessageRepository.save(old);
    }

    public void markMessageAsSent(Integer userId, Integer campaignMessageId) {
        CampaignMessage m = checkCampaignMessage(campaignMessageId);
        verifyOwnership(userId, m.getCampaign());
        if (m.getStatus() == MessageStatus.SENT) throw new ApiException("Campaign message is already marked as sent");
        m.setStatus(MessageStatus.SENT);
        campaignMessageRepository.save(m);
    }

    public void markMessageAsAnswered(Integer userId, Integer campaignMessageId) {
        CampaignMessage m = checkCampaignMessage(campaignMessageId);
        verifyOwnership(userId, m.getCampaign());
        if (m.getCustomerAnswer() == null) throw new ApiException("Campaign message has no customer answer");
        campaignMessageRepository.save(m);
    }

    public void deleteCampaignMessage(Integer userId, Integer campaignMessageId) {
        CampaignMessage m = checkCampaignMessage(campaignMessageId);
        verifyOwnership(userId, m.getCampaign());
        campaignMessageRepository.delete(m);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void verifyOwnership(Integer userId, Campaign campaign) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null || campaign.getBranch() == null
                || !campaign.getBranch().getStore().getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("You do not have permission to access this resource");
    }

    private void setCampaignMessage(CampaignMessage m, CampaignMessageRequestIn dto) {
        Campaign campaign     = checkCampaign(dto.getCampaignId());
        Customer customer     = checkCustomer(dto.getCustomerId());
        CustomerAnswer answer = checkCustomerAnswer(dto.getCustomerAnswerId());
        validateLinkedCustomerAnswer(answer, campaign, customer);
        m.setMessageText(dto.getMessageText());
        m.setDistanceKm(dto.getDistanceKm());
        m.setDurationMinutes(dto.getDurationMinutes());
        m.setDistanceText(dto.getDistanceText());
        m.setStatus(dto.getStatus());
        m.setSentAt(dto.getSentAt());
        m.setCampaign(campaign);
        m.setCustomer(customer);
        m.setCustomerAnswer(answer);
    }

    private void validateCampaignMessage(CampaignMessageRequestIn dto, Integer campaignMessageId) {
        if (Boolean.TRUE.equals(campaignMessageRepository.existsByCampaignIdAndCustomerId(
                dto.getCampaignId(), dto.getCustomerId())) && campaignMessageId == null)
            throw new ApiException("Campaign message already exists for this customer");
    }

    private CampaignMessage checkCampaignMessage(Integer id) {
        return campaignMessageRepository.findById(id)
                .orElseThrow(() -> new ApiException("Campaign message not found"));
    }

    private Campaign checkCampaign(Integer id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ApiException("Campaign not found"));
    }

    private Customer checkCustomer(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ApiException("Customer not found"));
    }

    private Customer checkCustomerByPhone(String phone) {
        if (phone == null || phone.isBlank()) throw new ApiException("Phone is required");
        String normalized = phone.replace("whatsapp:", "").trim();
        for (Customer c : customerRepository.findAll()) {
            if (c.getUser() != null && c.getUser().getPhone() != null
                    && c.getUser().getPhone().replace("whatsapp:", "").trim().equals(normalized))
                return c;
        }
        throw new ApiException("Customer not found");
    }

    private CustomerAnswer checkCustomerAnswer(Integer id) {
        if (id == null) return null;
        return customerAnswerRepository.findById(id)
                .orElseThrow(() -> new ApiException("Customer answer not found"));
    }

    private void validateLinkedCustomerAnswer(CustomerAnswer answer, Campaign campaign, Customer customer) {
        if (answer == null) return;
        if (!answer.getCampaign().getId().equals(campaign.getId())
                || !answer.getCustomer().getId().equals(customer.getId()))
            throw new ApiException("Customer answer does not belong to this campaign message");
    }

    private CampaignMessageResponseOut mapCampaignMessage(CampaignMessage m) {
        CampaignMessageResponseOut out = modelMapper.map(m, CampaignMessageResponseOut.class);
        out.setCampaignId(m.getCampaign() == null ? null : m.getCampaign().getId());
        out.setCustomerId(m.getCustomer() == null ? null : m.getCustomer().getId());
        out.setCustomerAnswerId(m.getCustomerAnswer() == null ? null : m.getCustomerAnswer().getId());
        return out;
    }
}