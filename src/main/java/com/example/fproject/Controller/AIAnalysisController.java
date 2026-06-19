package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.AIAnalysisIn;
import com.example.fproject.Service.AIAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai-analysis")
@RequiredArgsConstructor
public class AIAnalysisController {

    private final AIAnalysisService aiAnalysisService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllAIAnalyses() {
        return ResponseEntity.status(200).body(aiAnalysisService.getAllAIAnalyses());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getAIAnalysisById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAIAnalysisById(id));
    }

    @GetMapping("/get-by-sales-record/{salesRecordId}")
    public ResponseEntity<?> getAIAnalysisBySalesRecordId(@PathVariable Integer salesRecordId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAIAnalysisBySalesRecordId(salesRecordId));
    }

    @GetMapping("/peak-hours/{analysisId}")
    public ResponseEntity<?> getPeakHours(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getPeakHours(analysisId));
    }

    @GetMapping("/slow-hours/{analysisId}")
    public ResponseEntity<?> getSlowHours(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getSlowHours(analysisId));
    }

    @GetMapping("/confidence/{analysisId}")
    public ResponseEntity<?> getConfidence(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getConfidence(analysisId));
    }

    @GetMapping("/chart/{analysisId}")
    public ResponseEntity<?> getSalesChart(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getSalesChart(analysisId));
    }

    @GetMapping("/recommendations/{analysisId}")
    public ResponseEntity<?> getRecommendations(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getRecommendations(analysisId));
    }

    @GetMapping("/top-products/{analysisId}")
    public ResponseEntity<?> getTopProducts(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getTopProducts(analysisId));
    }

    @GetMapping("/low-products/{analysisId}")
    public ResponseEntity<?> getLowProducts(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getLowProducts(analysisId));
    }

    @GetMapping("/best-recommendation/{analysisId}")
    public ResponseEntity<?> getBestRecommendation(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getBestRecommendation(analysisId));
    }

    @GetMapping("/total-sales/{analysisId}")
    public ResponseEntity<?> getTotalSales(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getTotalSales(analysisId));
    }

    @GetMapping("/product-details/{analysisId}")
    public ResponseEntity<?> getProductDetails(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getProductDetails(analysisId));
    }

    @GetMapping("/summary/{analysisId}")
    public ResponseEntity<?> getAnalysisSummary(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAnalysisSummary(analysisId));
    }

    @GetMapping("/surplus-products/{analysisId}")
    public ResponseEntity<?> getSurplusProducts(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getSurplusProducts(analysisId));
    }

    @GetMapping("/seasonal-patterns/{analysisId}")
    public ResponseEntity<?> getSeasonalPatterns(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getSeasonalPatterns(analysisId));
    }

    @GetMapping("/ai-summary/{analysisId}")
    public ResponseEntity<?> getAiSummary(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAiSummary(analysisId));
    }

    @GetMapping("/suggested-campaign-ready/{analysisId}")
    public ResponseEntity<?> isSuggestedCampaignReady(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.isSuggestedCampaignReady(analysisId));
    }

    @GetMapping("/generated-at/{analysisId}")
    public ResponseEntity<?> getAnalysisGeneratedAt(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAnalysisGeneratedAt(analysisId));
    }

    @GetMapping("/branch-name/{analysisId}")
    public ResponseEntity<?> getAnalysisBranchName(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAnalysisBranchName(analysisId));
    }

    @GetMapping("/sales-record-info/{analysisId}")
    public ResponseEntity<?> getAnalysisSalesRecordInfo(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAnalysisSalesRecordInfo(analysisId));
    }

    @GetMapping("/main-opportunity/{analysisId}")
    public ResponseEntity<?> getAnalysisMainOpportunity(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAnalysisMainOpportunity(analysisId));
    }

    @GetMapping("/risk-note/{analysisId}")
    public ResponseEntity<?> getAnalysisRiskNote(@PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAnalysisRiskNote(analysisId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addAIAnalysis(@RequestBody @Valid AIAnalysisIn aiAnalysisIn) {
        aiAnalysisService.addAIAnalysis(aiAnalysisIn);
        return ResponseEntity.status(200).body(new ApiResponse("AI analysis added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateAIAnalysis(@PathVariable Integer id, @RequestBody @Valid AIAnalysisIn aiAnalysisIn) {
        aiAnalysisService.updateAIAnalysis(id, aiAnalysisIn);
        return ResponseEntity.status(200).body(new ApiResponse("AI analysis updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteAIAnalysis(@PathVariable Integer id) {
        aiAnalysisService.deleteAIAnalysis(id);
        return ResponseEntity.status(200).body(new ApiResponse("AI analysis deleted successfully"));
    }
}
