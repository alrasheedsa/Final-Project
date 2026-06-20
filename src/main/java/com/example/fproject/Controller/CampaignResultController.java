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

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<?> getCampaignResultByCampaign(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getCampaignResultByCampaign(campaignId));
    }

    @GetMapping("/total-sent/{campaignId}")
    public ResponseEntity<?> getTotalSent(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getTotalSent(campaignId));
    }

    @GetMapping("/total-answered/{campaignId}")
    public ResponseEntity<?> getTotalAnswered(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getTotalAnswered(campaignId));
    }

    @GetMapping("/correct-answers/{campaignId}")
    public ResponseEntity<?> getCorrectAnswers(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getCorrectAnswers(campaignId));
    }

    @GetMapping("/wrong-answers/{campaignId}")
    public ResponseEntity<?> getWrongAnswers(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getWrongAnswers(campaignId));
    }

    @GetMapping("/qr-used/{campaignId}")
    public ResponseEntity<?> getQRUsed(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getQRUsed(campaignId));
    }

    @GetMapping("/conversion-rate/{campaignId}")
    public ResponseEntity<?> getConversionRate(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getConversionRate(campaignId));
    }

    @GetMapping("/best-response-time/{campaignId}")
    public ResponseEntity<?> getBestResponseTime(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getBestResponseTime(campaignId));
    }

    @PostMapping("/generate-finished")
    public ResponseEntity<?> generateFinishedCampaignResults() {
        return ResponseEntity.status(200).body(campaignResultService.generateFinishedCampaignResults());
    }

    @GetMapping("/{campaignId}/dashboard")
    public ResponseEntity<?> getCampaignResultDashboard(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.getCampaignResultDashboard(campaignId));
    }

    @PostMapping("/generate/{campaignId}")
    public ResponseEntity<?> generateCampaignResult(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignResultService.generateCampaignResult(campaignId));
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
