package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.AIAnalysisIn;
import com.example.fproject.Model.User;
import com.example.fproject.Service.AIAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai-analysis")
@RequiredArgsConstructor
public class AIAnalysisController {

    private final AIAnalysisService aiAnalysisService;

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllAIAnalyses() {
        return ResponseEntity.status(200).body(aiAnalysisService.getAllAIAnalyses());
    }

    // STORE_OWNER
    @GetMapping("/get/{id}")
    public ResponseEntity<?> getAIAnalysisById(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAIAnalysisById(user.getId(), id));
    }

    // STORE_OWNER
    @GetMapping("/get-by-sales-record/{salesRecordId}")
    public ResponseEntity<?> getAIAnalysisBySalesRecordId(@AuthenticationPrincipal User user, @PathVariable Integer salesRecordId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAIAnalysisBySalesRecordId(user.getId(), salesRecordId));
    }

    // STORE_OWNER
    @GetMapping("/peak-hours/{analysisId}")
    public ResponseEntity<?> getPeakHours(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getPeakHours(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/slow-hours/{analysisId}")
    public ResponseEntity<?> getSlowHours(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getSlowHours(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/confidence/{analysisId}")
    public ResponseEntity<?> getConfidence(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getConfidence(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/chart/{analysisId}")
    public ResponseEntity<?> getSalesChart(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getSalesChart(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/recommendations/{analysisId}")
    public ResponseEntity<?> getRecommendations(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getRecommendations(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/top-products/{analysisId}")
    public ResponseEntity<?> getTopProducts(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getTopProducts(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/low-products/{analysisId}")
    public ResponseEntity<?> getLowProducts(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getLowProducts(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/best-recommendation/{analysisId}")
    public ResponseEntity<?> getBestRecommendation(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getBestRecommendation(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/total-sales/{analysisId}")
    public ResponseEntity<?> getTotalSales(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getTotalSales(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/product-details/{analysisId}")
    public ResponseEntity<?> getProductDetails(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getProductDetails(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/summary/{analysisId}")
    public ResponseEntity<?> getAnalysisSummary(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAnalysisSummary(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/surplus-products/{analysisId}")
    public ResponseEntity<?> getSurplusProducts(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getSurplusProducts(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/seasonal-patterns/{analysisId}")
    public ResponseEntity<?> getSeasonalPatterns(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getSeasonalPatterns(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/ai-summary/{analysisId}")
    public ResponseEntity<?> getAiSummary(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAiSummary(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/suggested-campaign-ready/{analysisId}")
    public ResponseEntity<?> isSuggestedCampaignReady(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.isSuggestedCampaignReady(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/generated-at/{analysisId}")
    public ResponseEntity<?> getAnalysisGeneratedAt(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAnalysisGeneratedAt(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/branch-name/{analysisId}")
    public ResponseEntity<?> getAnalysisBranchName(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAnalysisBranchName(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/sales-record-info/{analysisId}")
    public ResponseEntity<?> getAnalysisSalesRecordInfo(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAnalysisSalesRecordInfo(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/main-opportunity/{analysisId}")
    public ResponseEntity<?> getAnalysisMainOpportunity(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAnalysisMainOpportunity(user.getId(), analysisId));
    }

    // STORE_OWNER
    @GetMapping("/risk-note/{analysisId}")
    public ResponseEntity<?> getAnalysisRiskNote(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAnalysisRiskNote(user.getId(), analysisId));
    }

    // STORE_OWNER
    @PostMapping("/add/sales-record/{salesRecordId}")
    public ResponseEntity<?> addAIAnalysis(@AuthenticationPrincipal User user,
                                           @PathVariable Integer salesRecordId,
                                           @RequestBody @Valid AIAnalysisIn aiAnalysisIn) {
        aiAnalysisService.addAIAnalysis(user.getId(), salesRecordId, aiAnalysisIn);
        return ResponseEntity.status(200).body(new ApiResponse("AI analysis added successfully"));
    }

    // STORE_OWNER
    @PutMapping("/update/{id}/sales-record/{salesRecordId}")
    public ResponseEntity<?> updateAIAnalysis(@AuthenticationPrincipal User user,
                                              @PathVariable Integer id,
                                              @PathVariable Integer salesRecordId,
                                              @RequestBody @Valid AIAnalysisIn aiAnalysisIn) {
        aiAnalysisService.updateAIAnalysis(user.getId(), id, salesRecordId, aiAnalysisIn);
        return ResponseEntity.status(200).body(new ApiResponse("AI analysis updated successfully"));
    }

    // STORE_OWNER
    @GetMapping("/latest/branch/{branchId}")
    public ResponseEntity<?> getLatestAIAnalysisByBranch(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getLatestAIAnalysisByBranch(user.getId(), branchId));
    }

    // STORE_OWNER
    @GetMapping("/{analysisId}/dashboard")
    public ResponseEntity<?> getAIAnalysisDashboard(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(aiAnalysisService.getAIAnalysisDashboard(user.getId(), analysisId));
    }

    // STORE_OWNER
    @PostMapping("/{analysisId}/send-email-summary")
    public ResponseEntity<?> sendAIAnalysisSummaryEmail(@AuthenticationPrincipal User user, @PathVariable Integer analysisId) {
        return ResponseEntity.status(200).body(new ApiResponse(aiAnalysisService.sendAIAnalysisSummaryEmail(user.getId(), analysisId)));
    }

    // STORE_OWNER
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteAIAnalysis(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        aiAnalysisService.deleteAIAnalysis(user.getId(), id);
        return ResponseEntity.status(200).body(new ApiResponse("AI analysis deleted successfully"));
    }
}