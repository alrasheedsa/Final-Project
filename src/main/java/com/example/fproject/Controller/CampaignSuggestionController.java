package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.CampaignSuggestionIn;
import com.example.fproject.Service.CampaignSuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/campaign-suggestion")
@RequiredArgsConstructor
public class CampaignSuggestionController {

    private final CampaignSuggestionService campaignSuggestionService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllCampaignSuggestions() {
        return ResponseEntity.status(200).body(campaignSuggestionService.getAllCampaignSuggestions());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getCampaignSuggestionById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(campaignSuggestionService.getCampaignSuggestionById(id));
    }

    @GetMapping("/get-by-ai-analysis/{aiAnalysisId}")
    public ResponseEntity<?> getCampaignSuggestionsByAIAnalysisId(@PathVariable Integer aiAnalysisId) {
        return ResponseEntity.status(200).body(campaignSuggestionService.getCampaignSuggestionsByAIAnalysisId(aiAnalysisId));
    }

    @PostMapping("/generate/{aiAnalysisId}")
    public ResponseEntity<?> generateCampaignSuggestions(@PathVariable Integer aiAnalysisId) {
        return ResponseEntity.status(200).body(campaignSuggestionService.generateCampaignSuggestions(aiAnalysisId));
    }

    @PostMapping("/regenerate/{aiAnalysisId}")
    public ResponseEntity<?> regenerateCampaignSuggestions(@PathVariable Integer aiAnalysisId) {
        return ResponseEntity.status(200).body(campaignSuggestionService.regenerateCampaignSuggestions(aiAnalysisId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addCampaignSuggestion(@RequestBody @Valid CampaignSuggestionIn campaignSuggestionIn) {
        campaignSuggestionService.addCampaignSuggestion(campaignSuggestionIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign suggestion added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCampaignSuggestion(@PathVariable Integer id, @RequestBody @Valid CampaignSuggestionIn campaignSuggestionIn) {
        campaignSuggestionService.updateCampaignSuggestion(id, campaignSuggestionIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign suggestion updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCampaignSuggestion(@PathVariable Integer id) {
        campaignSuggestionService.deleteCampaignSuggestion(id);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign suggestion deleted successfully"));
    }

    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveCampaignSuggestion(@PathVariable Integer id) {
        campaignSuggestionService.approveCampaignSuggestion(id);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign suggestion approved successfully"));
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectCampaignSuggestion(@PathVariable Integer id) {
        campaignSuggestionService.rejectCampaignSuggestion(id);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign suggestion rejected successfully"));
    }
}
