package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.CampaignRequestIn;
import com.example.fproject.Model.User;
import com.example.fproject.Service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllCampaigns() {
        return ResponseEntity.status(200).body(campaignService.getAllCampaigns());
    }

    // STORE_OWNER
    @GetMapping("/get/{campaignId}")
    public ResponseEntity<?> getCampaignById(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignById(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<?> getCampaignsByBranchId(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignsByBranchId(user.getId(), branchId));
    }

    // STORE_OWNER
    @GetMapping("/branch/{branchId}/active")
    public ResponseEntity<?> getActiveCampaignsByBranch(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(campaignService.getActiveCampaignsByBranch(user.getId(), branchId));
    }

    // STORE_OWNER
    @GetMapping("/branch/{branchId}/scheduled")
    public ResponseEntity<?> getScheduledCampaignsByBranch(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(campaignService.getScheduledCampaignsByBranch(user.getId(), branchId));
    }

    // STORE_OWNER
    @GetMapping("/branch/{branchId}/completed")
    public ResponseEntity<?> getCompletedCampaignsByBranch(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(campaignService.getCompletedCampaignsByBranch(user.getId(), branchId));
    }

    // STORE_OWNER
    @GetMapping("/details/{campaignId}")
    public ResponseEntity<?> getCampaignDetails(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignDetails(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/{campaignId}/dashboard")
    public ResponseEntity<?> getCampaignDashboard(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignDashboard(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/{campaignId}/qr-status")
    public ResponseEntity<?> getCampaignQRStatus(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignQRStatus(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/max-customers/{campaignId}")
    public ResponseEntity<?> getMaxCustomers(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getMaxCustomers(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/used-coupons/{campaignId}")
    public ResponseEntity<?> getUsedCoupons(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getUsedCoupons(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/remaining-coupons/{campaignId}")
    public ResponseEntity<?> getRemainingCoupons(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getRemainingCoupons(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/usage-rate/{campaignId}")
    public ResponseEntity<?> getUsageRate(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getUsageRate(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/timing/{campaignId}")
    public ResponseEntity<?> getCampaignTiming(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignTiming(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/type/{campaignId}")
    public ResponseEntity<?> getCampaignType(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignType(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/question/{campaignId}")
    public ResponseEntity<?> getCampaignQuestion(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignQuestion(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/source/{campaignId}")
    public ResponseEntity<?> getCampaignSource(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignSource(user.getId(), campaignId));
    }

    // STORE_OWNER
    @PostMapping("/create-from-suggestion")
    public ResponseEntity<?> createCampaignFromSuggestion(@AuthenticationPrincipal User user,
                                                          @RequestParam Integer suggestionId,
                                                          @RequestParam Integer branchId) {
        return ResponseEntity.status(200).body(campaignService.createCampaignFromSuggestion(user.getId(), suggestionId, branchId));
    }

    // STORE_OWNER
    @PostMapping("/add")
    public ResponseEntity<?> addCampaign(@AuthenticationPrincipal User user, @RequestBody @Valid CampaignRequestIn campaignRequestIn) {
        campaignService.addCampaign(user.getId(), campaignRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign added successfully"));
    }

    // STORE_OWNER
    @PutMapping("/approve/{campaignId}")
    public ResponseEntity<?> approveCampaign(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        campaignService.approveCampaign(user.getId(), campaignId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign approved successfully"));
    }

    // STORE_OWNER
    @PutMapping("/cancel/{campaignId}")
    public ResponseEntity<?> cancelCampaign(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        campaignService.cancelCampaign(user.getId(), campaignId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign canceled successfully"));
    }

    // STORE_OWNER
    @PutMapping("/start/{campaignId}")
    public ResponseEntity<?> startCampaign(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        campaignService.startCampaign(user.getId(), campaignId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign started successfully"));
    }

    // STORE_OWNER
    @PutMapping("/complete/{campaignId}")
    public ResponseEntity<?> completeCampaign(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        campaignService.completeCampaign(user.getId(), campaignId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign completed successfully"));
    }

    // STORE_OWNER
    @PutMapping("/stop/{campaignId}")
    public ResponseEntity<?> stopCampaign(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        campaignService.stopCampaign(user.getId(), campaignId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign stopped successfully"));
    }

    // STORE_OWNER
    @PostMapping("/send/{campaignId}")
    public ResponseEntity<?> sendCampaign(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        campaignService.sendCampaign(user.getId(), campaignId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign sent successfully"));
    }

    // ADMIN
    @PutMapping("/expire-finished")
    public ResponseEntity<?> expireFinishedCampaigns() {
        campaignService.expireFinishedCampaigns();
        return ResponseEntity.status(200).body(new ApiResponse("Finished campaigns expired successfully"));
    }

    // ADMIN
    @PutMapping("/check-finished")
    public ResponseEntity<?> checkFinishedCampaigns() {
        campaignService.expireFinishedCampaigns();
        return ResponseEntity.status(200).body(new ApiResponse("Finished campaigns checked successfully"));
    }

    // ADMIN
    @PutMapping("/start-ready")
    public ResponseEntity<?> startReadyCampaigns() {
        campaignService.startReadyCampaigns();
        return ResponseEntity.status(200).body(new ApiResponse("Ready campaigns started successfully"));
    }

    // STORE_OWNER
    @PutMapping("/update/{campaignId}")
    public ResponseEntity<?> updateCampaign(@AuthenticationPrincipal User user, @PathVariable Integer campaignId,
                                            @RequestBody @Valid CampaignRequestIn campaignRequestIn) {
        campaignService.updateCampaign(user.getId(), campaignId, campaignRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign updated successfully"));
    }

    // STORE_OWNER
    @DeleteMapping("/deleted/{campaignId}")
    public ResponseEntity<?> deleteCampaign(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        campaignService.deleteCampaign(user.getId(), campaignId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign deleted successfully"));
    }
}