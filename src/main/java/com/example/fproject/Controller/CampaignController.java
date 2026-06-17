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

    @PostMapping("/add")
    public ResponseEntity<?> addCampaign(@RequestBody @Valid CampaignRequestIn campaignRequestIn) {
        campaignService.addCampaign(campaignRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign added successfully"));
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
