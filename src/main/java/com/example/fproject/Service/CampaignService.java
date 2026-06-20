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
import com.example.fproject.Model.AIQuestion;
import com.example.fproject.Model.Branch;
import com.example.fproject.Model.Campaign;
import com.example.fproject.Model.CampaignMessage;
import com.example.fproject.Model.CampaignResult;
import com.example.fproject.Model.CampaignSuggestion;
import com.example.fproject.Model.Customer;
import com.example.fproject.Model.QRCode;
import com.example.fproject.Model.Subscription;
import com.example.fproject.Repository.AiQuestionRepository;
import com.example.fproject.Repository.BranchRepository;
import com.example.fproject.Repository.CampaignMessageRepository;
import com.example.fproject.Repository.CampaignResultRepository;
import com.example.fproject.Repository.CampaignRepository;
import com.example.fproject.Repository.CampaignSuggestionRepository;
import com.example.fproject.Repository.CustomerRepository;
import com.example.fproject.Repository.QRCodeRepository;
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
    private final GoogleMapService googleMapService;
    private final WhatsAppService whatsAppService;
    private final CampaignResultService campaignResultService;
    private final ModelMapper modelMapper;

    public List<CampaignResponseOut> getAllCampaigns() {
        List<CampaignResponseOut> campaigns = new ArrayList<>();
        for (Campaign campaign : campaignRepository.findAll()) {
            campaigns.add(mapCampaign(campaign));
        }
        return campaigns;
    }

    public CampaignResponseOut getCampaignById(Integer campaignId) {
        return mapCampaign(checkCampaign(campaignId));
    }

    @Transactional
    public CampaignResponseOut createCampaignFromSuggestion(Integer suggestionId, Integer branchId) {
        CampaignSuggestion campaignSuggestion = checkCampaignSuggestion(suggestionId, null);
        Branch branch = checkBranch(branchId);
        validateCampaignSuggestion(campaignSuggestion, branch, campaignSuggestion.getCampaignType());
        validateCampaignSuggestionTime(campaignSuggestion);

        Campaign campaign = new Campaign();
        campaign.setTitle(campaignSuggestion.getTitle());
        campaign.setDescription(campaignSuggestion.getDescription());
        campaign.setOfferText(campaignSuggestion.getOfferText());
        campaign.setCampaignType(campaignSuggestion.getCampaignType());
        campaign.setStartDateTime(buildSuggestedDateTime(campaignSuggestion.getSuggestedStartDate(),
                campaignSuggestion.getSuggestedStartTime()));
        campaign.setEndDateTime(buildSuggestedDateTime(campaignSuggestion.getSuggestedEndDate(),
                campaignSuggestion.getSuggestedEndTime()));
        campaign.setTargetCustomersCount(campaignSuggestion.getTargetCustomersCount());
        campaign.setSentCount(0);
        campaign.setRedeemedCount(0);
        campaign.setStatus(CampaignStatus.PENDING);
        campaign.setBranch(branch);
        campaign.setCampaignSuggestion(campaignSuggestion);

        return mapCampaign(campaignRepository.save(campaign));
    }

    public List<CampaignResponseOut> getCampaignsByBranchId(Integer branchId) {
        checkBranch(branchId);
        List<CampaignResponseOut> campaigns = new ArrayList<>();
        for (Campaign campaign : campaignRepository.findAllByBranchId(branchId)) {
            campaigns.add(mapCampaign(campaign));
        }
        return campaigns;
    }

    public List<CampaignResponseOut> getActiveCampaignsByBranch(Integer branchId) {
        return getCampaignsByBranchAndStatus(branchId, CampaignStatus.ACTIVE);
    }

    public List<CampaignResponseOut> getScheduledCampaignsByBranch(Integer branchId) {
        return getCampaignsByBranchAndStatus(branchId, CampaignStatus.APPROVED);
    }

    public List<CampaignResponseOut> getCompletedCampaignsByBranch(Integer branchId) {
        return getCampaignsByBranchAndStatus(branchId, CampaignStatus.COMPLETED);
    }

    @Transactional
    public void addCampaign(CampaignRequestIn dto) {
        validateCampaign(dto);
        Campaign campaign = new Campaign();
        setCampaign(campaign, dto);
        campaign.setStatus(CampaignStatus.PENDING);
        campaignRepository.save(campaign);
    }

    @Transactional
    public void updateCampaign(Integer campaignId, CampaignRequestIn dto) {
        validateCampaign(dto);
        Campaign old = checkCampaign(campaignId);
        if (old.getStatus() == CampaignStatus.ACTIVE || old.getStatus() == CampaignStatus.EXPIRED
                || old.getStatus() == CampaignStatus.COMPLETED || old.getStatus() == CampaignStatus.CANCELED
                || old.getStatus() == CampaignStatus.STOPPED) {
            throw new ApiException("Cannot update campaign after it starts or ends");
        }
        setCampaign(old, dto);
        campaignRepository.save(old);
    }

    public void deleteCampaign(Integer campaignId) {
        // Business note: this CRUD method exists for full coverage, but real workflow may change status instead of hard delete.
        Campaign campaign = checkCampaign(campaignId);
        if (campaign.getCampaignMessages() != null && !campaign.getCampaignMessages().isEmpty()) {
            throw new ApiException("Cannot delete campaign because it has campaign messages");
        }
        if (campaign.getQrCode() != null) {
            throw new ApiException("Cannot delete campaign because it has a QR code");
        }
        if (campaign.getCampaignResult() != null) {
            throw new ApiException("Cannot delete campaign because it has a campaign result");
        }
        campaignRepository.delete(campaign);
    }

    @Transactional
    public void approveCampaign(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        if (campaign.getStatus() != CampaignStatus.PENDING) {
            throw new ApiException("Only pending campaign can be approved");
        }
        validateCampaignReady(campaign);
        campaign.setStatus(CampaignStatus.APPROVED);
        campaignRepository.save(campaign);
    }

    @Transactional
    public void cancelCampaign(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        if (campaign.getStatus() == CampaignStatus.EXPIRED || campaign.getStatus() == CampaignStatus.COMPLETED) {
            throw new ApiException("Cannot cancel ended campaign");
        }
        campaign.setStatus(CampaignStatus.CANCELED);
        campaignRepository.save(campaign);
    }

    @Transactional
    public void startCampaign(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        validateCampaignCanStart(campaign);
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaignRepository.save(campaign);
    }

    @Transactional
    public void completeCampaign(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        if (campaign.getStatus() != CampaignStatus.ACTIVE && campaign.getStatus() != CampaignStatus.APPROVED) {
            throw new ApiException("Only active or approved campaign can be completed");
        }
        campaign.setStatus(CampaignStatus.COMPLETED);
        campaignRepository.save(campaign);
    }

    @Transactional
    public void stopCampaign(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new ApiException("Only active campaign can be stopped");
        }
        campaign.setStatus(CampaignStatus.STOPPED);
        campaignRepository.save(campaign);
    }

    @Transactional
    public void sendCampaign(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        validateCampaignCanSend(campaign);

        QRCode qrCode = qrCodeRepository.findQRCodeByCampaignId(campaignId);
        if (qrCode == null) {
            throw new ApiException("Campaign must have a QR code before sending");
        }

        Branch branch = campaign.getBranch();
        Integer sentCount = 0;
        for (Customer customer : customerRepository.findCustomersByLocationConsentTrue()) {
            if (sentCount >= campaign.getTargetCustomersCount()) {
                break;
            }
            if (customer.getUser() == null || customer.getUser().getPhone() == null
                    || customer.getUser().getPhone().isBlank()) {
                continue;
            }
            if (!isCustomerInsideRadius(customer, branch)) {
                continue;
            }
            if (Boolean.TRUE.equals(campaignMessageRepository.existsByCampaignIdAndCustomerId(campaign.getId(), customer.getId()))) {
                continue;
            }
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
            CampaignMessage campaignMessage = new CampaignMessage();
            campaignMessage.setMessageText(messageText);
            campaignMessage.setDistanceKm(route.distanceKm());
            campaignMessage.setDurationMinutes(route.durationMinutes());
            campaignMessage.setDistanceText(route.distanceText());
            campaignMessage.setStatus(MessageStatus.SENT);
            campaignMessage.setSentAt(LocalDateTime.now());
            campaignMessage.setCampaign(campaign);
            campaignMessage.setCustomer(customer);
            campaignMessageRepository.save(campaignMessage);
            sentCount++;
        }

        if (sentCount == 0) {
            throw new ApiException("No customers found inside branch radius");
        }

        campaign.setSentCount(campaign.getSentCount() + sentCount);
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaignRepository.save(campaign);
    }

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

    @Transactional
    public void startReadyCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        for (Campaign campaign : campaignRepository.findAllByStatus(CampaignStatus.APPROVED)) {
            if (campaign.getStartDateTime() == null || campaign.getEndDateTime() == null) {
                continue;
            }
            if (!campaign.getStartDateTime().isAfter(now) && campaign.getEndDateTime().isAfter(now)) {
                sendCampaign(campaign.getId());
            }
        }
    }

    public CampaignDetailsOut getCampaignDetails(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        Branch branch = campaign.getBranch();
        return new CampaignDetailsOut(
                campaign.getId(),
                campaign.getTitle(),
                campaign.getDescription(),
                campaign.getOfferText(),
                campaign.getCampaignType(),
                campaign.getStatus(),
                campaign.getStartDateTime(),
                campaign.getEndDateTime(),
                campaign.getTargetCustomersCount(),
                campaign.getSentCount(),
                campaign.getRedeemedCount(),
                calculateRemainingCoupons(campaign),
                calculateUsageRate(campaign),
                branch == null ? null : branch.getId(),
                branch == null ? null : branch.getName(),
                branch == null || branch.getStore() == null ? null : branch.getStore().getName(),
                campaign.getCampaignSuggestion() == null ? null : campaign.getCampaignSuggestion().getId(),
                campaign.getAiQuestion() == null ? null : campaign.getAiQuestion().getId(),
                campaign.getQrCode() == null ? null : campaign.getQrCode().getId()
        );
    }

    public Map<String, Object> getCampaignDashboard(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);

        Map<String, Object> dashboard = new HashMap<>();

        dashboard.put("campaignDetails", getCampaignDetails(campaignId));
        dashboard.put("maxCustomers", getMaxCustomers(campaignId));
        dashboard.put("usedCoupons", getUsedCoupons(campaignId));
        dashboard.put("remainingCoupons", getRemainingCoupons(campaignId));
        dashboard.put("usageRate", getUsageRate(campaignId));
        dashboard.put("timing", getCampaignTiming(campaignId));
        dashboard.put("type", getCampaignType(campaignId));
        dashboard.put("question", getCampaignQuestion(campaignId));
        dashboard.put("source", getCampaignSource(campaignId));
        dashboard.put("qrStatus", getCampaignQRStatus(campaignId));

        if (campaign.getCampaignResult() == null) {
            dashboard.put("campaignResult", null);
        } else {
            dashboard.put("campaignResult", campaignResultService.getCampaignResultByCampaign(campaignId));
        }

        return dashboard;
    }

    public Map<String, Object> getCampaignQRStatus(Integer campaignId) {
        checkCampaign(campaignId);

        QRCode qrCode = qrCodeRepository.findQRCodeByCampaignId(campaignId);

        Map<String, Object> qrStatus = new HashMap<>();

        if (qrCode == null) {
            qrStatus.put("hasQRCode", false);
            qrStatus.put("status", "NOT_GENERATED");
            qrStatus.put("qrCodeId", null);
            qrStatus.put("code", null);
            qrStatus.put("usedCount", 0);
            qrStatus.put("maxUsageCount", 0);
            qrStatus.put("remainingUsage", 0);
            return qrStatus;
        }

        Integer usedCount = qrCode.getUsedCount() == null ? 0 : qrCode.getUsedCount();
        Integer maxUsageCount = qrCode.getMaxUsageCount() == null ? 0 : qrCode.getMaxUsageCount();

        qrStatus.put("hasQRCode", true);
        qrStatus.put("status", qrCode.getStatus());
        qrStatus.put("qrCodeId", qrCode.getId());
        qrStatus.put("code", qrCode.getCode());
        qrStatus.put("usedCount", usedCount);
        qrStatus.put("maxUsageCount", maxUsageCount);
        qrStatus.put("remainingUsage", maxUsageCount - usedCount);

        return qrStatus;
    }

    public ValueOut getMaxCustomers(Integer campaignId) {
        return new ValueOut(checkCampaign(campaignId).getTargetCustomersCount());
    }

    public ValueOut getUsedCoupons(Integer campaignId) {
        return new ValueOut(checkCampaign(campaignId).getRedeemedCount());
    }

    public ValueOut getRemainingCoupons(Integer campaignId) {
        return new ValueOut(calculateRemainingCoupons(checkCampaign(campaignId)));
    }

    public ValueOut getUsageRate(Integer campaignId) {
        return new ValueOut(calculateUsageRate(checkCampaign(campaignId)));
    }

    public CampaignTimingOut getCampaignTiming(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        Duration duration = Duration.between(campaign.getStartDateTime(), campaign.getEndDateTime());
        return new CampaignTimingOut(campaign.getStartDateTime(), campaign.getEndDateTime(),
                duration.toHours() + " hours");
    }

    public ValueOut getCampaignType(Integer campaignId) {
        return new ValueOut(checkCampaign(campaignId).getCampaignType());
    }

    public ValueOut getCampaignQuestion(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        if (campaign.getCampaignType() != CampaignType.QUESTION_BASED || campaign.getAiQuestion() == null) {
            return new ValueOut(null);
        }
        return new ValueOut(campaign.getAiQuestion().getQuestionText());
    }

    public ValueOut getCampaignSource(Integer campaignId) {
        Campaign campaign = checkCampaign(campaignId);
        if (campaign.getCampaignSuggestion() == null) {
            return new ValueOut("Manual");
        }
        return new ValueOut("AI suggestion");
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
        CampaignSuggestion campaignSuggestion = checkCampaignSuggestion(dto.getCampaignSuggestionId(), campaign.getId());
        validateCampaignSuggestion(campaignSuggestion, branch, dto.getCampaignType());
        campaign.setBranch(branch);
        campaign.setCampaignSuggestion(campaignSuggestion);
        campaign.setAiQuestion(checkAiQuestion(dto.getAiQuestionId(), campaign.getId(), dto.getCampaignType()));
        campaign.setCampaignResult(checkCampaignResult(dto.getCampaignResultId(), campaign.getId()));
    }

    private void validateCampaign(CampaignRequestIn dto) {
        if (dto.getStartDateTime() == null || dto.getEndDateTime() == null) {
            throw new ApiException("Campaign start and end time are required");
        }
        if (!dto.getEndDateTime().isAfter(dto.getStartDateTime())) {
            throw new ApiException("Campaign end time must be after start time");
        }
        if (dto.getSentCount() > dto.getTargetCustomersCount()) {
            throw new ApiException("Sent count cannot be greater than target customers count");
        }
        if (dto.getRedeemedCount() > dto.getSentCount()) {
            throw new ApiException("Redeemed count cannot be greater than sent count");
        }
        if (dto.getRedeemedCount() > dto.getTargetCustomersCount()) {
            throw new ApiException("Redeemed count cannot be greater than target customers count");
        }
        if (dto.getCampaignType() == CampaignType.DIRECT_OFFER && dto.getAiQuestionId() != null) {
            throw new ApiException("Direct offer campaign cannot have an AI question");
        }
    }

    private List<CampaignResponseOut> getCampaignsByBranchAndStatus(Integer branchId, CampaignStatus status) {
        checkBranch(branchId);
        List<CampaignResponseOut> campaigns = new ArrayList<>();
        for (Campaign campaign : campaignRepository.findAllByBranchIdAndStatus(branchId, status)) {
            campaigns.add(mapCampaign(campaign));
        }
        return campaigns;
    }

    private LocalDateTime buildSuggestedDateTime(LocalDate date, LocalTime time) {
        return date.atTime(time);
    }

    private void validateCampaignReady(Campaign campaign) {
        if (campaign.getBranch() == null) {
            throw new ApiException("Campaign branch is required");
        }
        validateBranchReady(campaign.getBranch());
        if (campaign.getCampaignType() == CampaignType.QUESTION_BASED && campaign.getAiQuestion() == null) {
            throw new ApiException("Question based campaign must have an AI question");
        }
        if (qrCodeRepository.findQRCodeByCampaignId(campaign.getId()) == null) {
            throw new ApiException("Campaign must have a QR code before approval");
        }
        if (campaign.getEndDateTime().isBefore(LocalDateTime.now())) {
            throw new ApiException("Campaign end time is expired");
        }
    }

    private void validateCampaignCanStart(Campaign campaign) {
        if (campaign.getStatus() != CampaignStatus.APPROVED) {
            throw new ApiException("Only approved campaign can be started");
        }
        validateCampaignReady(campaign);
    }

    private void validateCampaignCanSend(Campaign campaign) {
        if (campaign.getStatus() != CampaignStatus.APPROVED && campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new ApiException("Campaign must be approved before sending");
        }
        validateCampaignReady(campaign);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(campaign.getStartDateTime())) {
            throw new ApiException("Campaign cannot be sent before start time");
        }
        if (now.isAfter(campaign.getEndDateTime())) {
            throw new ApiException("Campaign end time is expired");
        }
        if (campaign.getSentCount() >= campaign.getTargetCustomersCount()) {
            throw new ApiException("Campaign already reached target customers count");
        }
    }

    private void validateBranchReady(Branch branch) {
        if (branch.getStatus() != StoreStatus.ACTIVE) {
            throw new ApiException("Branch must be active before sending campaign");
        }
        if (branch.getLatitude() == null || branch.getLongitude() == null) {
            throw new ApiException("Branch latitude and longitude are required");
        }
        if (branch.getCampaignRadiusMeters() == null || branch.getCampaignRadiusMeters() <= 0) {
            throw new ApiException("Branch campaign radius is required");
        }
        if (branch.getStore() == null) {
            throw new ApiException("Branch store is required");
        }
        if (branch.getStore().getStatus() != StoreStatus.ACTIVE) {
            throw new ApiException("Store must be active before sending campaign");
        }
        validateActiveSubscription(branch);
    }

    private void validateActiveSubscription(Branch branch) {
        if (branch.getStore().getStoreOwner() == null
                || branch.getStore().getStoreOwner().getSubscriptions() == null
                || branch.getStore().getStoreOwner().getSubscriptions().isEmpty()) {
            throw new ApiException("Store owner must have an active subscription");
        }
        for (Subscription subscription : branch.getStore().getStoreOwner().getSubscriptions()) {
            if (subscription.getStatus() == SubscriptionStatus.ACTIVE
                    && subscription.getEndDate() != null
                    && !subscription.getEndDate().isBefore(LocalDate.now())) {
                return;
            }
        }
        throw new ApiException("Store owner must have an active subscription");
    }

    private Boolean isCustomerInsideRadius(Customer customer, Branch branch) {
        if (customer.getLatitude() == null || customer.getLongitude() == null
                || branch.getLatitude() == null || branch.getLongitude() == null
                || branch.getCampaignRadiusMeters() == null) {
            return false;
        }
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
        if (campaign.getTargetCustomersCount() == null || campaign.getTargetCustomersCount() == 0) {
            return 0.0;
        }
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
        if (branch == null) {
            throw new ApiException("Branch not found");
        }
        return branch;
    }

    private CampaignSuggestion checkCampaignSuggestion(Integer campaignSuggestionId, Integer campaignId) {
        if (campaignSuggestionId == null) return null;
        CampaignSuggestion campaignSuggestion = campaignSuggestionRepository.findById(campaignSuggestionId)
                .orElseThrow(() -> new ApiException("Campaign suggestion not found"));
        if (campaignSuggestion.getApprovalStatus() != SuggestionStatus.APPROVED) {
            throw new ApiException("Campaign suggestion must be approved before creating a campaign");
        }
        if (campaignSuggestion.getCampaign() != null && !campaignSuggestion.getCampaign().getId().equals(campaignId)) {
            throw new ApiException("Campaign suggestion is already linked to another campaign");
        }
        return campaignSuggestion;
    }

    private void validateCampaignSuggestion(CampaignSuggestion campaignSuggestion, Branch branch, CampaignType campaignType) {
        if (campaignSuggestion == null) return;
        if (campaignSuggestion.getAiAnalysis() == null
                || campaignSuggestion.getAiAnalysis().getSalesRecord() == null
                || campaignSuggestion.getAiAnalysis().getSalesRecord().getBranch() == null) {
            throw new ApiException("Campaign suggestion must be linked to a branch sales analysis");
        }
        Branch suggestionBranch = campaignSuggestion.getAiAnalysis().getSalesRecord().getBranch();
        if (!suggestionBranch.getId().equals(branch.getId())) {
            throw new ApiException("Campaign suggestion does not belong to this branch");
        }
        if (!campaignSuggestion.getCampaignType().equals(campaignType)) {
            throw new ApiException("Campaign type must match campaign suggestion type");
        }
    }

    private void validateCampaignSuggestionTime(CampaignSuggestion campaignSuggestion) {
        if (campaignSuggestion.getSuggestedStartDate() == null || campaignSuggestion.getSuggestedEndDate() == null) {
            throw new ApiException("Campaign suggestion date is required");
        }
        if (campaignSuggestion.getSuggestedStartTime() == null || campaignSuggestion.getSuggestedEndTime() == null) {
            throw new ApiException("Campaign suggestion time is required");
        }
        LocalDateTime startDateTime = buildSuggestedDateTime(campaignSuggestion.getSuggestedStartDate(),
                campaignSuggestion.getSuggestedStartTime());
        LocalDateTime endDateTime = buildSuggestedDateTime(campaignSuggestion.getSuggestedEndDate(),
                campaignSuggestion.getSuggestedEndTime());
        if (!endDateTime.isAfter(startDateTime)) {
            throw new ApiException("Campaign suggestion end time must be after start time");
        }
    }

    private AIQuestion checkAiQuestion(Integer aiQuestionId, Integer campaignId, CampaignType campaignType) {
        if (aiQuestionId == null) return null;
        if (campaignType != CampaignType.QUESTION_BASED) {
            throw new ApiException("AI question can only be linked to question based campaign");
        }
        AIQuestion aiQuestion = aiQuestionRepository.findById(aiQuestionId)
                .orElseThrow(() -> new ApiException("AI question not found"));
        if (aiQuestion.getCampaign() != null && !aiQuestion.getCampaign().getId().equals(campaignId)) {
            throw new ApiException("AI question is already linked to another campaign");
        }
        return aiQuestion;
    }

    private CampaignResult checkCampaignResult(Integer campaignResultId, Integer campaignId) {
        if (campaignResultId == null) return null;
        CampaignResult campaignResult = campaignResultRepository.findById(campaignResultId)
                .orElseThrow(() -> new ApiException("Campaign result not found"));
        if (campaignResult.getCampaign() != null && !campaignResult.getCampaign().getId().equals(campaignId)) {
            throw new ApiException("Campaign result is already linked to another campaign");
        }
        return campaignResult;
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
