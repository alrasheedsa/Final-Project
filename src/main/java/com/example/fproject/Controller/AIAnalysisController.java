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
