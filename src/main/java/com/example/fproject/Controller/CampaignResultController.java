package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.CampaignResultRequestIn;
import com.example.fproject.Model.User;
import com.example.fproject.Service.CampaignResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/campaign-results")
@RequiredArgsConstructor
public class CampaignResultController {

    private final CampaignResultService campaignResultService;

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllCampaignResults() {
        return ResponseEntity.status(200).body(campaignResultService.getAllCampaignResults());
    }

    // STORE_OWNER
    @GetMapping("/get/{campaignResultId}")
    public ResponseEntity<?> getCampaignResultById(@AuthenticationPrincipal User user, @PathVariable Integer campaignResultId) {
        return ResponseEntity.status(200).body(campaignResultService.getCampaignResultById(user.getId(), campaignResultId));
    }

    // STORE_OWNER
    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<?> getCampaignResultByCampaign(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getCampaignResultByCampaign(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/total-sent/{campaignId}")
    public ResponseEntity<?> getTotalSent(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getTotalSent(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/total-answered/{campaignId}")
    public ResponseEntity<?> getTotalAnswered(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getTotalAnswered(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/correct-answers/{campaignId}")
    public ResponseEntity<?> getCorrectAnswers(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getCorrectAnswers(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/wrong-answers/{campaignId}")
    public ResponseEntity<?> getWrongAnswers(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getWrongAnswers(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/qr-used/{campaignId}")
    public ResponseEntity<?> getQRUsed(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getQRUsed(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/conversion-rate/{campaignId}")
    public ResponseEntity<?> getConversionRate(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getConversionRate(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/best-response-time/{campaignId}")
    public ResponseEntity<?> getBestResponseTime(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getBestResponseTime(user.getId(), campaignId));
    }

    // ADMIN
    @PostMapping("/generate-finished")
    public ResponseEntity<?> generateFinishedCampaignResults() {
        return ResponseEntity.status(200).body(campaignResultService.generateFinishedCampaignResults());
    }

    // STORE_OWNER
    @GetMapping("/{campaignId}/dashboard")
    public ResponseEntity<?> getCampaignResultDashboard(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getCampaignResultDashboard(user.getId(), campaignId));
    }

    // STORE_OWNER
    @PostMapping("/generate/{campaignId}")
    public ResponseEntity<?> generateCampaignResult(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.generateCampaignResult(user.getId(), campaignId));
    }

    // STORE_OWNER
    @PostMapping("/add")
    public ResponseEntity<?> addCampaignResult(@AuthenticationPrincipal User user, @RequestBody @Valid CampaignResultRequestIn campaignResultRequestIn) {
        campaignResultService.addCampaignResult(user.getId(), campaignResultRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign result added successfully"));
    }

    // STORE_OWNER
    @PutMapping("/update/{campaignResultId}")
    public ResponseEntity<?> updateCampaignResult(@AuthenticationPrincipal User user,
                                                  @PathVariable Integer campaignResultId,
                                                  @RequestBody @Valid CampaignResultRequestIn campaignResultRequestIn) {
        campaignResultService.updateCampaignResult(user.getId(), campaignResultId, campaignResultRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign result updated successfully"));
    }

    // STORE_OWNER
    @DeleteMapping("/deleted/{campaignResultId}")
    public ResponseEntity<?> deleteCampaignResult(@AuthenticationPrincipal User user, @PathVariable Integer campaignResultId) {
        campaignResultService.deleteCampaignResult(user.getId(), campaignResultId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign result deleted successfully"));
    }
}