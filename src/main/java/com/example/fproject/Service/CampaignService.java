package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.CampaignRequestIn;
import com.example.fproject.DTO.OUT.CampaignDetailsOut;
import com.example.fproject.DTO.OUT.CampaignResponseOut;
import com.example.fproject.DTO.OUT.CampaignTimingOut;
import com.example.fproject.DTO.OUT.ValueOut;
import com.example.fproject.Enum.CampaignStatus;
import com.example.fproject.Enum.CampaignType;
import com.example.fproject.Enum.MessageStatus;
import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Enum.SuggestionStatus;
import com.example.fproject.Enum.SubscriptionStatus;
import com.example.fproject.Model.*;
import com.example.fproject.Repository.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final BranchRepository branchRepository;
    private final CampaignSuggestionRepository campaignSuggestionRepository;
    private final AiQuestionRepository aiQuestionRepository;
    private final CampaignResultRepository campaignResultRepository;
    private final CampaignMessageRepository campaignMessageRepository;
    private final CustomerRepository customerRepository;
    private final QRCodeRepository qrCodeRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final GoogleMapService googleMapService;
    private final WhatsAppService whatsAppService;
    private final CampaignResultService campaignResultService;
    private final ModelMapper modelMapper;

    // ADMIN
    public List<CampaignResponseOut> getAllCampaigns() {
        List<CampaignResponseOut> campaigns = new ArrayList<>();
        for (Campaign campaign : campaignRepository.findAll()) campaigns.add(mapCampaign(campaign));
        return campaigns;
    }

    // STORE_OWNER
    public CampaignResponseOut getCampaignById(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        return mapCampaign(campaign);
    }

    // STORE_OWNER
    @Transactional
    public CampaignResponseOut createCampaignFromSuggestion(Integer userId, Integer suggestionId, Integer branchId) {
        CampaignSuggestion suggestion = checkCampaignSuggestion(suggestionId, null);
        Branch branch = checkBranch(branchId);
        verifyBranchOwnership(userId, branch);
        validateCampaignSuggestion(suggestion, branch, suggestion.getCampaignType());
        validateCampaignSuggestionTime(suggestion);

        Campaign campaign = new Campaign();
        campaign.setTitle(suggestion.getTitle());
        campaign.setDescription(suggestion.getDescription());
        campaign.setOfferText(suggestion.getOfferText());
        campaign.setCampaignType(suggestion.getCampaignType());
        campaign.setStartDateTime(buildSuggestedDateTime(suggestion.getSuggestedStartDate(), suggestion.getSuggestedStartTime()));
        campaign.setEndDateTime(buildSuggestedDateTime(suggestion.getSuggestedEndDate(), suggestion.getSuggestedEndTime()));
        campaign.setTargetCustomersCount(suggestion.getTargetCustomersCount());
        campaign.setSentCount(0);
        campaign.setRedeemedCount(0);
        campaign.setStatus(CampaignStatus.PENDING);
        campaign.setBranch(branch);
        campaign.setCampaignSuggestion(suggestion);

        return mapCampaign(campaignRepository.save(campaign));
    }

    // STORE_OWNER
    public List<CampaignResponseOut> getCampaignsByBranchId(Integer userId, Integer branchId) {
        Branch branch = checkBranch(branchId);
        verifyBranchOwnership(userId, branch);
        List<CampaignResponseOut> campaigns = new ArrayList<>();
        for (Campaign c : campaignRepository.findAllByBranchId(branchId)) campaigns.add(mapCampaign(c));
        return campaigns;
    }

    // STORE_OWNER
    public List<CampaignResponseOut> getActiveCampaignsByBranch(Integer userId, Integer branchId) {
        return getCampaignsByBranchAndStatus(userId, branchId, CampaignStatus.ACTIVE);
    }

    // STORE_OWNER
    public List<CampaignResponseOut> getScheduledCampaignsByBranch(Integer userId, Integer branchId) {
        return getCampaignsByBranchAndStatus(userId, branchId, CampaignStatus.APPROVED);
    }

    // STORE_OWNER
    public List<CampaignResponseOut> getCompletedCampaignsByBranch(Integer userId, Integer branchId) {
        return getCampaignsByBranchAndStatus(userId, branchId, CampaignStatus.COMPLETED);
    }

    // STORE_OWNER
    @Transactional
    public void addCampaign(Integer userId, CampaignRequestIn dto) {
        validateCampaign(dto);
        Branch branch = checkBranch(dto.getBranchId());
        verifyBranchOwnership(userId, branch);
        Campaign campaign = new Campaign();
        setCampaign(campaign, dto);
        campaign.setStatus(CampaignStatus.PENDING);
        campaignRepository.save(campaign);
    }

    // STORE_OWNER
    @Transactional
    public void updateCampaign(Integer userId, Integer campaignId, CampaignRequestIn dto) {
        validateCampaign(dto);
        Campaign old = checkCampaign(campaignId);
        verifyOwnership(userId, old);
        if (old.getStatus() == CampaignStatus.ACTIVE || old.getStatus() == CampaignStatus.EXPIRED
                || old.getStatus() == CampaignStatus.COMPLETED || old.getStatus() == CampaignStatus.CANCELED
                || old.getStatus() == CampaignStatus.STOPPED)
            throw new ApiException("Cannot update campaign after it starts or ends");
        setCampaign(old, dto);
        campaignRepository.save(old);
    }

    // STORE_OWNER
    public void deleteCampaign(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        if (campaign.getCampaignMessages() != null && !campaign.getCampaignMessages().isEmpty())
            throw new ApiException("Cannot delete campaign because it has campaign messages");
        if (campaign.getQrCode() != null)
            throw new ApiException("Cannot delete campaign because it has a QR code");
        if (campaign.getCampaignResult() != null)
            throw new ApiException("Cannot delete campaign because it has a campaign result");
        campaignRepository.delete(campaign);
    }

    // STORE_OWNER
    @Transactional
    public void approveCampaign(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        if (campaign.getStatus() != CampaignStatus.PENDING)
            throw new ApiException("Only pending campaign can be approved");
        validateCampaignReady(campaign);
        campaign.setStatus(CampaignStatus.APPROVED);
        campaignRepository.save(campaign);
    }

    // STORE_OWNER
    @Transactional
    public void cancelCampaign(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        if (campaign.getStatus() == CampaignStatus.EXPIRED || campaign.getStatus() == CampaignStatus.COMPLETED)
            throw new ApiException("Cannot cancel ended campaign");
        campaign.setStatus(CampaignStatus.CANCELED);
        campaignRepository.save(campaign);
    }

    // STORE_OWNER
    @Transactional
    public void startCampaign(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        validateCampaignCanStart(campaign);
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaignRepository.save(campaign);
    }

    // STORE_OWNER
    @Transactional
    public void completeCampaign(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        if (campaign.getStatus() != CampaignStatus.ACTIVE && campaign.getStatus() != CampaignStatus.APPROVED)
            throw new ApiException("Only active or approved campaign can be completed");
        campaign.setStatus(CampaignStatus.COMPLETED);
        campaignRepository.save(campaign);
    }

    // STORE_OWNER
    @Transactional
    public void stopCampaign(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        if (campaign.getStatus() != CampaignStatus.ACTIVE)
            throw new ApiException("Only active campaign can be stopped");
        campaign.setStatus(CampaignStatus.STOPPED);
        campaignRepository.save(campaign);
    }

    // STORE_OWNER
    @Transactional
    public void sendCampaign(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        validateCampaignCanSend(campaign);

        QRCode qrCode = qrCodeRepository.findQRCodeByCampaignId(campaignId);
        if (qrCode == null) throw new ApiException("Campaign must have a QR code before sending");

        Branch branch = campaign.getBranch();
        Integer sentCount = 0;

        for (Customer customer : customerRepository.findAll()) {
            if (sentCount >= campaign.getTargetCustomersCount()) break;
            if (customer.getUser() == null || customer.getUser().getPhone() == null
                    || customer.getUser().getPhone().isBlank()) continue;
            if (!isCustomerInsideRadius(customer, branch)) continue;
            if (Boolean.TRUE.equals(campaignMessageRepository.existsByCampaignIdAndCustomerId(campaign.getId(), customer.getId()))) continue;

            GoogleMapService.RouteResult route = googleMapService.calculateRoute(
                    customer.getLatitude(), customer.getLongitude(),
                    branch.getLatitude(), branch.getLongitude()
            );
            String phone = customer.getUser().getPhone();
            String storeName = branch.getStore().getName();
            String messageText;

            if (campaign.getCampaignType() == CampaignType.QUESTION_BASED) {
                AIQuestion question = campaign.getAiQuestion();
                whatsAppService.sendQuestionMessage(phone, storeName, question.getQuestionText(),
                        question.getOptionA(), question.getOptionB(), question.getOptionC());
                messageText = question.getQuestionText();
            } else {
                whatsAppService.sendDirectOfferMessage(phone, storeName, branch.getName(), campaign.getTitle(),
                        campaign.getOfferText(), buildCampaignTime(campaign), branch.getLocationUrl(),
                        route.distanceText(), route.durationMinutes(), qrCode.getCode());
                messageText = campaign.getOfferText();
            }

            CampaignMessage msg = new CampaignMessage();
            msg.setMessageText(messageText);
            msg.setDistanceKm(route.distanceKm());
            msg.setDurationMinutes(route.durationMinutes());
            msg.setDistanceText(route.distanceText());
            msg.setStatus(MessageStatus.SENT);
            msg.setSentAt(LocalDateTime.now());
            msg.setCampaign(campaign);
            msg.setCustomer(customer);
            campaignMessageRepository.save(msg);
            sentCount++;
        }

        if (sentCount == 0) throw new ApiException("No customers found inside branch radius");

        campaign.setSentCount(campaign.getSentCount() + sentCount);
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaignRepository.save(campaign);
    }

    // ADMIN — يشغّل تلقائياً بدون userId
    @Transactional
    public void expireFinishedCampaigns() {
        for (Campaign campaign : campaignRepository.findAllByStatus(CampaignStatus.ACTIVE)) {
            if (campaign.getEndDateTime() != null && campaign.getEndDateTime().isBefore(LocalDateTime.now())) {
                campaign.setStatus(CampaignStatus.EXPIRED);
                campaignRepository.save(campaign);
                campaignResultService.generateCampaignResult(campaign.getId());
            }
        }
    }

    // ADMIN
    @Transactional
    public void startReadyCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        for (Campaign campaign : campaignRepository.findAllByStatus(CampaignStatus.APPROVED)) {
            if (campaign.getStartDateTime() == null || campaign.getEndDateTime() == null) continue;
            if (!campaign.getStartDateTime().isAfter(now) && campaign.getEndDateTime().isAfter(now))
                sendCampaign(campaign.getBranch().getStore().getStoreOwner().getUser().getId(), campaign.getId());
        }
    }

    // STORE_OWNER
    public CampaignDetailsOut getCampaignDetails(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        Branch branch = campaign.getBranch();
        return new CampaignDetailsOut(
                campaign.getId(), campaign.getTitle(), campaign.getDescription(), campaign.getOfferText(),
                campaign.getCampaignType(), campaign.getStatus(), campaign.getStartDateTime(), campaign.getEndDateTime(),
                campaign.getTargetCustomersCount(), campaign.getSentCount(), campaign.getRedeemedCount(),
                calculateRemainingCoupons(campaign), calculateUsageRate(campaign),
                branch == null ? null : branch.getId(),
                branch == null ? null : branch.getName(),
                branch == null || branch.getStore() == null ? null : branch.getStore().getName(),
                campaign.getCampaignSuggestion() == null ? null : campaign.getCampaignSuggestion().getId(),
                campaign.getAiQuestion() == null ? null : campaign.getAiQuestion().getId(),
                campaign.getQrCode() == null ? null : campaign.getQrCode().getId()
        );
    }

    // STORE_OWNER
    public Map<String, Object> getCampaignDashboard(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("campaignDetails", getCampaignDetails(userId, campaignId));
        dashboard.put("maxCustomers", getMaxCustomers(userId, campaignId));
        dashboard.put("usedCoupons", getUsedCoupons(userId, campaignId));
        dashboard.put("remainingCoupons", getRemainingCoupons(userId, campaignId));
        dashboard.put("usageRate", getUsageRate(userId, campaignId));
        dashboard.put("timing", getCampaignTiming(userId, campaignId));
        dashboard.put("type", getCampaignType(userId, campaignId));
        dashboard.put("question", getCampaignQuestion(userId, campaignId));
        dashboard.put("source", getCampaignSource(userId, campaignId));
        dashboard.put("qrStatus", getCampaignQRStatus(userId, campaignId));
        dashboard.put("campaignResult", campaign.getCampaignResult() == null ? null
                : campaignResultService.getCampaignResultByCampaign(userId, campaignId));
        return dashboard;
    }

    // STORE_OWNER
    public Map<String, Object> getCampaignQRStatus(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);

        QRCode qrCode = qrCodeRepository.findQRCodeByCampaignId(campaignId);
        Map<String, Object> qrStatus = new HashMap<>();

        if (qrCode == null) {
            qrStatus.put("hasQRCode", false); qrStatus.put("status", "NOT_GENERATED");
            qrStatus.put("qrCodeId", null);   qrStatus.put("code", null);
            qrStatus.put("usedCount", 0);     qrStatus.put("maxUsageCount", 0);
            qrStatus.put("remainingUsage", 0);
            return qrStatus;
        }

        Integer usedCount     = qrCode.getUsedCount() == null ? 0 : qrCode.getUsedCount();
        Integer maxUsageCount = qrCode.getMaxUsageCount() == null ? 0 : qrCode.getMaxUsageCount();

        qrStatus.put("hasQRCode", true);  qrStatus.put("status", qrCode.getStatus());
        qrStatus.put("qrCodeId", qrCode.getId()); qrStatus.put("code", qrCode.getCode());
        qrStatus.put("usedCount", usedCount); qrStatus.put("maxUsageCount", maxUsageCount);
        qrStatus.put("remainingUsage", maxUsageCount - usedCount);
        return qrStatus;
    }

    // STORE_OWNER
    public ValueOut getMaxCustomers(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        return new ValueOut(campaign.getTargetCustomersCount());
    }

    // STORE_OWNER
    public ValueOut getUsedCoupons(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        return new ValueOut(campaign.getRedeemedCount());
    }

    // STORE_OWNER
    public ValueOut getRemainingCoupons(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        return new ValueOut(calculateRemainingCoupons(campaign));
    }

    // STORE_OWNER
    public ValueOut getUsageRate(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        return new ValueOut(calculateUsageRate(campaign));
    }

    // STORE_OWNER
    public CampaignTimingOut getCampaignTiming(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        Duration duration = Duration.between(campaign.getStartDateTime(), campaign.getEndDateTime());
        return new CampaignTimingOut(campaign.getStartDateTime(), campaign.getEndDateTime(), duration.toHours() + " hours");
    }

    // STORE_OWNER
    public ValueOut getCampaignType(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        return new ValueOut(campaign.getCampaignType());
    }

    // STORE_OWNER
    public ValueOut getCampaignQuestion(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        if (campaign.getCampaignType() != CampaignType.QUESTION_BASED || campaign.getAiQuestion() == null)
            return new ValueOut(null);
        return new ValueOut(campaign.getAiQuestion().getQuestionText());
    }

    // STORE_OWNER
    public ValueOut getCampaignSource(Integer userId, Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        verifyOwnership(userId, campaign);
        return campaign.getCampaignSuggestion() == null ? new ValueOut("Manual") : new ValueOut("AI suggestion");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void verifyOwnership(Integer userId, Campaign campaign) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null || campaign.getBranch() == null
                || !campaign.getBranch().getStore().getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("You do not have permission to access this resource");
    }

    private void verifyBranchOwnership(Integer userId, Branch branch) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null || !branch.getStore().getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("You do not have permission to access this branch");
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
        CampaignSuggestion suggestion = checkCampaignSuggestion(dto.getCampaignSuggestionId(), campaign.getId());
        validateCampaignSuggestion(suggestion, branch, dto.getCampaignType());
        campaign.setBranch(branch);
        campaign.setCampaignSuggestion(suggestion);
        campaign.setAiQuestion(checkAiQuestion(dto.getAiQuestionId(), campaign.getId(), dto.getCampaignType()));
        campaign.setCampaignResult(checkCampaignResult(dto.getCampaignResultId(), campaign.getId()));
    }

    private void validateCampaign(CampaignRequestIn dto) {
        if (dto.getStartDateTime() == null || dto.getEndDateTime() == null)
            throw new ApiException("Campaign start and end time are required");
        if (!dto.getEndDateTime().isAfter(dto.getStartDateTime()))
            throw new ApiException("Campaign end time must be after start time");
        if (dto.getSentCount() > dto.getTargetCustomersCount())
            throw new ApiException("Sent count cannot be greater than target customers count");
        if (dto.getRedeemedCount() > dto.getSentCount())
            throw new ApiException("Redeemed count cannot be greater than sent count");
        if (dto.getRedeemedCount() > dto.getTargetCustomersCount())
            throw new ApiException("Redeemed count cannot be greater than target customers count");
        if (dto.getCampaignType() == CampaignType.DIRECT_OFFER && dto.getAiQuestionId() != null)
            throw new ApiException("Direct offer campaign cannot have an AI question");
    }

    private List<CampaignResponseOut> getCampaignsByBranchAndStatus(Integer userId, Integer branchId, CampaignStatus status) {
        Branch branch = checkBranch(branchId);
        verifyBranchOwnership(userId, branch);
        List<CampaignResponseOut> campaigns = new ArrayList<>();
        for (Campaign c : campaignRepository.findAllByBranchIdAndStatus(branchId, status)) campaigns.add(mapCampaign(c));
        return campaigns;
    }

    private LocalDateTime buildSuggestedDateTime(LocalDate date, LocalTime time) {
        return date.atTime(time);
    }

    private void validateCampaignReady(Campaign campaign) {
        if (campaign.getBranch() == null) throw new ApiException("Campaign branch is required");
        validateBranchReady(campaign.getBranch());
        if (campaign.getCampaignType() == CampaignType.QUESTION_BASED && campaign.getAiQuestion() == null)
            throw new ApiException("Question based campaign must have an AI question");
        if (qrCodeRepository.findQRCodeByCampaignId(campaign.getId()) == null)
            throw new ApiException("Campaign must have a QR code before approval");
        if (campaign.getEndDateTime().isBefore(LocalDateTime.now()))
            throw new ApiException("Campaign end time is expired");
    }

    private void validateCampaignCanStart(Campaign campaign) {
        if (campaign.getStatus() != CampaignStatus.APPROVED)
            throw new ApiException("Only approved campaign can be started");
        validateCampaignReady(campaign);
    }

    private void validateCampaignCanSend(Campaign campaign) {
        if (campaign.getStatus() != CampaignStatus.APPROVED && campaign.getStatus() != CampaignStatus.ACTIVE)
            throw new ApiException("Campaign must be approved before sending");
        validateCampaignReady(campaign);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(campaign.getStartDateTime())) throw new ApiException("Campaign cannot be sent before start time");
        if (now.isAfter(campaign.getEndDateTime())) throw new ApiException("Campaign end time is expired");
        if (campaign.getSentCount() >= campaign.getTargetCustomersCount())
            throw new ApiException("Campaign already reached target customers count");
    }

    private void validateBranchReady(Branch branch) {
        if (branch.getStatus() != StoreStatus.ACTIVE) throw new ApiException("Branch must be active before sending campaign");
        if (branch.getLatitude() == null || branch.getLongitude() == null)
            throw new ApiException("Branch latitude and longitude are required");
        if (branch.getCampaignRadiusMeters() == null || branch.getCampaignRadiusMeters() <= 0)
            throw new ApiException("Branch campaign radius is required");
        if (branch.getStore() == null) throw new ApiException("Branch store is required");
        if (branch.getStore().getStatus() != StoreStatus.ACTIVE)
            throw new ApiException("Store must be active before sending campaign");
        validateActiveSubscription(branch);
    }

    private void validateActiveSubscription(Branch branch) {
        if (branch.getStore().getStoreOwner() == null
                || branch.getStore().getStoreOwner().getSubscriptions() == null
                || branch.getStore().getStoreOwner().getSubscriptions().isEmpty())
            throw new ApiException("Store owner must have an active subscription");
        for (Subscription s : branch.getStore().getStoreOwner().getSubscriptions()) {
            if (s.getStatus() == SubscriptionStatus.ACTIVE && s.getEndDate() != null
                    && !s.getEndDate().isBefore(LocalDate.now())) return;
        }
        throw new ApiException("Store owner must have an active subscription");
    }

    private Boolean isCustomerInsideRadius(Customer customer, Branch branch) {
        if (customer.getLatitude() == null || customer.getLongitude() == null
                || branch.getLatitude() == null || branch.getLongitude() == null
                || branch.getCampaignRadiusMeters() == null) return false;
        Double distanceKm = calculateDistance(customer.getLatitude(), customer.getLongitude(),
                branch.getLatitude(), branch.getLongitude());
        return distanceKm * 1000 <= branch.getCampaignRadiusMeters();
    }

    private Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        double earthRadiusKm = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private Integer calculateRemainingCoupons(Campaign campaign) {
        return Math.max(campaign.getTargetCustomersCount() - campaign.getRedeemedCount(), 0);
    }

    private Double calculateUsageRate(Campaign campaign) {
        if (campaign.getTargetCustomersCount() == null || campaign.getTargetCustomersCount() == 0) return 0.0;
        return (campaign.getRedeemedCount() * 100.0) / campaign.getTargetCustomersCount();
    }

    private String buildCampaignTime(Campaign campaign) {
        return campaign.getStartDateTime() + " - " + campaign.getEndDateTime();
    }

    private Campaign checkCampaign(Integer campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException("Campaign not found"));
    }

    private Branch checkBranch(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);
        if (branch == null) throw new ApiException("Branch not found");
        return branch;
    }

    private CampaignSuggestion checkCampaignSuggestion(Integer campaignSuggestionId, Integer campaignId) {
        if (campaignSuggestionId == null) return null;
        CampaignSuggestion s = campaignSuggestionRepository.findById(campaignSuggestionId)
                .orElseThrow(() -> new ApiException("Campaign suggestion not found"));
        if (s.getApprovalStatus() != SuggestionStatus.APPROVED)
            throw new ApiException("Campaign suggestion must be approved before creating a campaign");
        if (s.getCampaign() != null && !s.getCampaign().getId().equals(campaignId))
            throw new ApiException("Campaign suggestion is already linked to another campaign");
        return s;
    }

    private void validateCampaignSuggestion(CampaignSuggestion s, Branch branch, CampaignType campaignType) {
        if (s == null) return;
        if (s.getAiAnalysis() == null || s.getAiAnalysis().getSalesRecord() == null
                || s.getAiAnalysis().getSalesRecord().getBranch() == null)
            throw new ApiException("Campaign suggestion must be linked to a branch sales analysis");
        if (!s.getAiAnalysis().getSalesRecord().getBranch().getId().equals(branch.getId()))
            throw new ApiException("Campaign suggestion does not belong to this branch");
        if (!s.getCampaignType().equals(campaignType))
            throw new ApiException("Campaign type must match campaign suggestion type");
    }

    private void validateCampaignSuggestionTime(CampaignSuggestion s) {
        if (s.getSuggestedStartDate() == null || s.getSuggestedEndDate() == null)
            throw new ApiException("Campaign suggestion date is required");
        if (s.getSuggestedStartTime() == null || s.getSuggestedEndTime() == null)
            throw new ApiException("Campaign suggestion time is required");
        LocalDateTime start = buildSuggestedDateTime(s.getSuggestedStartDate(), s.getSuggestedStartTime());
        LocalDateTime end   = buildSuggestedDateTime(s.getSuggestedEndDate(), s.getSuggestedEndTime());
        if (!end.isAfter(start)) throw new ApiException("Campaign suggestion end time must be after start time");
    }

    private AIQuestion checkAiQuestion(Integer aiQuestionId, Integer campaignId, CampaignType campaignType) {
        if (aiQuestionId == null) return null;
        if (campaignType != CampaignType.QUESTION_BASED)
            throw new ApiException("AI question can only be linked to question based campaign");
        AIQuestion q = aiQuestionRepository.findById(aiQuestionId)
                .orElseThrow(() -> new ApiException("AI question not found"));
        if (q.getCampaign() != null && !q.getCampaign().getId().equals(campaignId))
            throw new ApiException("AI question is already linked to another campaign");
        return q;
    }

    private CampaignResult checkCampaignResult(Integer campaignResultId, Integer campaignId) {
        if (campaignResultId == null) return null;
        CampaignResult r = campaignResultRepository.findById(campaignResultId)
                .orElseThrow(() -> new ApiException("Campaign result not found"));
        if (r.getCampaign() != null && !r.getCampaign().getId().equals(campaignId))
            throw new ApiException("Campaign result is already linked to another campaign");
        return r;
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