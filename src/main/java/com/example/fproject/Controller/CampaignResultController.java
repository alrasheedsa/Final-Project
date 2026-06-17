package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.CampaignResultRequestIn;
import com.example.fproject.Service.CampaignResultService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/campaign-results")
@RequiredArgsConstructor
public class CampaignResultController {

    private final CampaignResultService campaignResultService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllCampaignResults() {
        return ResponseEntity.status(200).body(campaignResultService.getAllCampaignResults());
    }

    @GetMapping("/get/{campaignResultId}")
    public ResponseEntity<?> getCampaignResultById(@PathVariable Integer campaignResultId) {
        return ResponseEntity.status(200).body(campaignResultService.getCampaignResultById(campaignResultId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addCampaignResult(@RequestBody @Valid CampaignResultRequestIn campaignResultRequestIn) {
        campaignResultService.addCampaignResult(campaignResultRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign result added successfully"));
    }

    @PutMapping("/update/{campaignResultId}")
    public ResponseEntity<?> updateCampaignResult(@PathVariable Integer campaignResultId,
                                                  @RequestBody @Valid CampaignResultRequestIn campaignResultRequestIn) {
        // Business note: endpoint exists for CRUD coverage; workflow may restrict final-result updates after reporting.
        campaignResultService.updateCampaignResult(campaignResultId, campaignResultRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign result updated successfully"));
    }

    @DeleteMapping("/deleted/{campaignResultId}")
    public ResponseEntity<?> deleteCampaignResult(@PathVariable Integer campaignResultId) {
        // Business note: endpoint exists for CRUD coverage; workflow may keep final results for monthly reports.
        campaignResultService.deleteCampaignResult(campaignResultId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign result deleted successfully"));
    }
}
