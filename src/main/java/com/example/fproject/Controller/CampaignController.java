package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.CampaignRequestIn;
import com.example.fproject.Service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllCampaigns() {
        return ResponseEntity.status(200).body(campaignService.getAllCampaigns());
    }

    @GetMapping("/get/{campaignId}")
    public ResponseEntity<?> getCampaignById(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignById(campaignId));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<?> getCampaignsByBranchId(@PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignsByBranchId(branchId));
    }

    @GetMapping("/branch/{branchId}/active")
    public ResponseEntity<?> getActiveCampaignsByBranch(@PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(campaignService.getActiveCampaignsByBranch(branchId));
    }

    @GetMapping("/branch/{branchId}/scheduled")
    public ResponseEntity<?> getScheduledCampaignsByBranch(@PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(campaignService.getScheduledCampaignsByBranch(branchId));
    }

    @GetMapping("/branch/{branchId}/completed")
    public ResponseEntity<?> getCompletedCampaignsByBranch(@PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(campaignService.getCompletedCampaignsByBranch(branchId));
    }

    @GetMapping("/details/{campaignId}")
    public ResponseEntity<?> getCampaignDetails(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignDetails(campaignId));
    }

    @GetMapping("/{campaignId}/dashboard")
    public ResponseEntity<?> getCampaignDashboard(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignDashboard(campaignId));
    }

    @GetMapping("/{campaignId}/qr-status")
    public ResponseEntity<?> getCampaignQRStatus(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignQRStatus(campaignId));
    }

    @GetMapping("/max-customers/{campaignId}")
    public ResponseEntity<?> getMaxCustomers(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getMaxCustomers(campaignId));
    }

    @GetMapping("/used-coupons/{campaignId}")
    public ResponseEntity<?> getUsedCoupons(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getUsedCoupons(campaignId));
    }

    @GetMapping("/remaining-coupons/{campaignId}")
    public ResponseEntity<?> getRemainingCoupons(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getRemainingCoupons(campaignId));
    }

    @GetMapping("/usage-rate/{campaignId}")
    public ResponseEntity<?> getUsageRate(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getUsageRate(campaignId));
    }

    @GetMapping("/timing/{campaignId}")
    public ResponseEntity<?> getCampaignTiming(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignTiming(campaignId));
    }

    @GetMapping("/type/{campaignId}")
    public ResponseEntity<?> getCampaignType(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignType(campaignId));
    }

    @GetMapping("/question/{campaignId}")
    public ResponseEntity<?> getCampaignQuestion(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignQuestion(campaignId));
    }

    @GetMapping("/source/{campaignId}")
    public ResponseEntity<?> getCampaignSource(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignService.getCampaignSource(campaignId));
    }

    @PostMapping("/create-from-suggestion")
    public ResponseEntity<?> createCampaignFromSuggestion(@RequestParam Integer suggestionId,
                                                          @RequestParam Integer branchId) {
        return ResponseEntity.status(200).body(campaignService.createCampaignFromSuggestion(suggestionId, branchId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addCampaign(@RequestBody @Valid CampaignRequestIn campaignRequestIn) {
        campaignService.addCampaign(campaignRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign added successfully"));
    }

    @PutMapping("/approve/{campaignId}")
    public ResponseEntity<?> approveCampaign(@PathVariable Integer campaignId) {
        campaignService.approveCampaign(campaignId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign approved successfully"));
    }

    @PutMapping("/cancel/{campaignId}")
    public ResponseEntity<?> cancelCampaign(@PathVariable Integer campaignId) {
        campaignService.cancelCampaign(campaignId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign canceled successfully"));
    }

    @PutMapping("/start/{campaignId}")
    public ResponseEntity<?> startCampaign(@PathVariable Integer campaignId) {
        campaignService.startCampaign(campaignId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign started successfully"));
    }

    @PutMapping("/complete/{campaignId}")
    public ResponseEntity<?> completeCampaign(@PathVariable Integer campaignId) {
        campaignService.completeCampaign(campaignId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign completed successfully"));
    }

    @PutMapping("/stop/{campaignId}")
    public ResponseEntity<?> stopCampaign(@PathVariable Integer campaignId) {
        campaignService.stopCampaign(campaignId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign stopped successfully"));
    }

    @PostMapping("/send/{campaignId}")
    public ResponseEntity<?> sendCampaign(@PathVariable Integer campaignId) {
        campaignService.sendCampaign(campaignId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign sent successfully"));
    }

    @PutMapping("/expire-finished")
    public ResponseEntity<?> expireFinishedCampaigns() {
        campaignService.expireFinishedCampaigns();
        return ResponseEntity.status(200).body(new ApiResponse("Finished campaigns expired successfully"));
    }

    @PutMapping("/update/{campaignId}")
    public ResponseEntity<?> updateCampaign(@PathVariable Integer campaignId,
                                            @RequestBody @Valid CampaignRequestIn campaignRequestIn) {
        campaignService.updateCampaign(campaignId, campaignRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign updated successfully"));
    }

    @DeleteMapping("/deleted/{campaignId}")
    public ResponseEntity<?> deleteCampaign(@PathVariable Integer campaignId) {
        // Business note: endpoint exists for CRUD coverage; workflow may cancel campaign instead of hard delete.
        campaignService.deleteCampaign(campaignId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign deleted successfully"));
    }
}
