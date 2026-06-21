package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.CampaignSuggestionIn;
import com.example.fproject.Model.User;
import com.example.fproject.Service.CampaignSuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/campaign-suggestion")
@RequiredArgsConstructor
public class CampaignSuggestionController {

    private final CampaignSuggestionService campaignSuggestionService;

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllCampaignSuggestions() {
        return ResponseEntity.status(200).body(campaignSuggestionService.getAllCampaignSuggestions());
    }

    // STORE_OWNER
    @GetMapping("/get/{id}")
    public ResponseEntity<?> getCampaignSuggestionById(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        return ResponseEntity.status(200).body(campaignSuggestionService.getCampaignSuggestionById(user.getId(), id));
    }

    // STORE_OWNER
    @GetMapping("/get-by-ai-analysis/{aiAnalysisId}")
    public ResponseEntity<?> getCampaignSuggestionsByAIAnalysisId(@AuthenticationPrincipal User user, @PathVariable Integer aiAnalysisId) {
        return ResponseEntity.status(200).body(campaignSuggestionService.getCampaignSuggestionsByAIAnalysisId(user.getId(), aiAnalysisId));
    }

    // STORE_OWNER
    @PostMapping("/generate/{aiAnalysisId}")
    public ResponseEntity<?> generateCampaignSuggestions(@AuthenticationPrincipal User user, @PathVariable Integer aiAnalysisId) {
        return ResponseEntity.status(200).body(campaignSuggestionService.generateCampaignSuggestions(user.getId(), aiAnalysisId));
    }

    // STORE_OWNER
    @PostMapping("/regenerate/{aiAnalysisId}")
    public ResponseEntity<?> regenerateCampaignSuggestions(@AuthenticationPrincipal User user, @PathVariable Integer aiAnalysisId) {
        return ResponseEntity.status(200).body(campaignSuggestionService.regenerateCampaignSuggestions(user.getId(), aiAnalysisId));
    }

    // STORE_OWNER
    @PostMapping("/add/analysis/{aiAnalysisId}")
    public ResponseEntity<?> addCampaignSuggestion(@AuthenticationPrincipal User user,
                                                   @PathVariable Integer aiAnalysisId,
                                                   @RequestBody @Valid CampaignSuggestionIn campaignSuggestionIn) {
        campaignSuggestionService.addCampaignSuggestion(user.getId(), aiAnalysisId, campaignSuggestionIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign suggestion added successfully"));
    }

    // STORE_OWNER
    @PutMapping("/update/{id}/analysis/{aiAnalysisId}")
    public ResponseEntity<?> updateCampaignSuggestion(@AuthenticationPrincipal User user,
                                                      @PathVariable Integer id,
                                                      @PathVariable Integer aiAnalysisId,
                                                      @RequestBody @Valid CampaignSuggestionIn campaignSuggestionIn) {
        campaignSuggestionService.updateCampaignSuggestion(user.getId(), id, aiAnalysisId, campaignSuggestionIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign suggestion updated successfully"));
    }

    // STORE_OWNER
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCampaignSuggestion(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        campaignSuggestionService.deleteCampaignSuggestion(user.getId(), id);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign suggestion deleted successfully"));
    }

    // STORE_OWNER
    @GetMapping("/approved/analysis/{analysisId}")
    public ResponseEntity<?> getApprovedSuggestionByAnalysis(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(campaignSuggestionService.getApprovedSuggestionByAnalysis(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/pending/analysis/{analysisId}")
    public ResponseEntity<?> getPendingSuggestionsByAnalysis(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(campaignSuggestionService.getPendingSuggestionsByAnalysis(user.getId(), analysisId));
    }

    // STORE_OWNER
    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveCampaignSuggestion(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        campaignSuggestionService.approveCampaignSuggestion(user.getId(), id);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign suggestion approved successfully"));
    }

    // STORE_OWNER
    @PostMapping("/{suggestionId}/send-approval-email")
    public ResponseEntity<?> sendApprovedCampaignSuggestionEmail(@AuthenticationPrincipal User user, @PathVariable Integer suggestionId) {
        return ResponseEntity.status(200).body(new ApiResponse(campaignSuggestionService.sendApprovedCampaignSuggestionEmail(user.getId(), suggestionId)));
    }

    // STORE_OWNER
    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectCampaignSuggestion(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        campaignSuggestionService.rejectCampaignSuggestion(user.getId(), id);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign suggestion rejected successfully"));
    }
}