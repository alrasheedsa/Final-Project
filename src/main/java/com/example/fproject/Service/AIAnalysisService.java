package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.AIAnalysisIn;
import com.example.fproject.DTO.OUT.AIAnalysisOut;
import com.example.fproject.Model.AIAnalysis;
import com.example.fproject.Model.SalesRecord;
import com.example.fproject.Repository.AIAnalysisRepository;
import com.example.fproject.Repository.SalesRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AIAnalysisService {

    private final AIAnalysisRepository aiAnalysisRepository;
    private final SalesRecordRepository salesRecordRepository;
    private final OpenAiService openAiService;

    public List<AIAnalysisOut> getAllAIAnalyses() {
        List<AIAnalysis> aiAnalyses = aiAnalysisRepository.findAll();
        List<AIAnalysisOut> aiAnalysisOuts = new ArrayList<>();

        for (AIAnalysis aiAnalysis : aiAnalyses) {
            aiAnalysisOuts.add(convertToOut(aiAnalysis));
        }

        return aiAnalysisOuts;
    }

    public AIAnalysisOut getAIAnalysisById(Integer id) {
        AIAnalysis aiAnalysis = aiAnalysisRepository.findAIAnalysisById(id);

        if (aiAnalysis == null) {
            throw new ApiException("AI analysis not found");
        }

        return convertToOut(aiAnalysis);
    }

    public AIAnalysisOut getAIAnalysisBySalesRecordId(Integer salesRecordId) {
        SalesRecord salesRecord = salesRecordRepository.findSalesRecordById(salesRecordId);

        if (salesRecord == null) {
            throw new ApiException("Sales record not found");
        }

        AIAnalysis aiAnalysis = aiAnalysisRepository.findAIAnalysisBySalesRecord_Id(salesRecordId);

        if (aiAnalysis == null) {
            throw new ApiException("AI analysis not found for this sales record");
        }

        return convertToOut(aiAnalysis);
    }

    public void addAIAnalysis(AIAnalysisIn aiAnalysisIn) {
        validateAIAnalysisIn(aiAnalysisIn);

        SalesRecord salesRecord = salesRecordRepository.findSalesRecordById(aiAnalysisIn.getSalesRecordId());

        if (salesRecord == null) {
            throw new ApiException("Sales record not found");
        }

        Boolean exists = aiAnalysisRepository.existsBySalesRecord_Id(aiAnalysisIn.getSalesRecordId());

        if (Boolean.TRUE.equals(exists)) {
            throw new ApiException("This sales record already has AI analysis");
        }

        AIAnalysis aiAnalysis = new AIAnalysis();

        aiAnalysis.setTopProducts(aiAnalysisIn.getTopProducts());
        aiAnalysis.setLowProducts(aiAnalysisIn.getLowProducts());
        aiAnalysis.setPeakHours(aiAnalysisIn.getPeakHours());
        aiAnalysis.setSlowHours(aiAnalysisIn.getSlowHours());
        aiAnalysis.setSurplusProducts(aiAnalysisIn.getSurplusProducts());
        aiAnalysis.setSeasonalPatterns(aiAnalysisIn.getSeasonalPatterns());
        aiAnalysis.setRecommendation(aiAnalysisIn.getRecommendation());
        aiAnalysis.setAiSummary(aiAnalysisIn.getAiSummary());
        aiAnalysis.setAnalyzedAt(LocalDateTime.now());
        aiAnalysis.setSalesRecord(salesRecord);

        aiAnalysisRepository.save(aiAnalysis);
    }

    public void generateAIAnalysisFromSalesRecord(Integer salesRecordId, String salesData) {
        SalesRecord salesRecord = salesRecordRepository.findSalesRecordById(salesRecordId);

        if (salesRecord == null) {
            throw new ApiException("Sales record not found");
        }

        if (salesData == null || salesData.isBlank()) {
            throw new ApiException("Sales data is required");
        }

        Boolean exists = aiAnalysisRepository.existsBySalesRecord_Id(salesRecordId);

        if (Boolean.TRUE.equals(exists)) {
            throw new ApiException("This sales record already has AI analysis");
        }

        OpenAiService.AIAnalysisResult result = openAiService.analyzeSalesDataForAIAnalysis(salesData);

        AIAnalysis aiAnalysis = new AIAnalysis();

        aiAnalysis.setTopProducts(result.topProducts());
        aiAnalysis.setLowProducts(result.lowProducts());
        aiAnalysis.setPeakHours(result.peakHours());
        aiAnalysis.setSlowHours(result.slowHours());
        aiAnalysis.setSurplusProducts(result.surplusProducts());
        aiAnalysis.setSeasonalPatterns(result.seasonalPatterns());
        aiAnalysis.setRecommendation(result.recommendation());
        aiAnalysis.setAiSummary(result.aiSummary());
        aiAnalysis.setAnalyzedAt(LocalDateTime.now());
        aiAnalysis.setSalesRecord(salesRecord);

        aiAnalysisRepository.save(aiAnalysis);
    }

    public void updateAIAnalysis(Integer id, AIAnalysisIn aiAnalysisIn) {
        validateAIAnalysisIn(aiAnalysisIn);

        AIAnalysis oldAIAnalysis = aiAnalysisRepository.findAIAnalysisById(id);

        if (oldAIAnalysis == null) {
            throw new ApiException("AI analysis not found");
        }

        SalesRecord salesRecord = salesRecordRepository.findSalesRecordById(aiAnalysisIn.getSalesRecordId());

        if (salesRecord == null) {
            throw new ApiException("Sales record not found");
        }

        Boolean changedSalesRecord =
                !oldAIAnalysis.getSalesRecord().getId().equals(aiAnalysisIn.getSalesRecordId());

        if (changedSalesRecord) {
            Boolean exists = aiAnalysisRepository.existsBySalesRecord_Id(aiAnalysisIn.getSalesRecordId());

            if (Boolean.TRUE.equals(exists)) {
                throw new ApiException("Another AI analysis already exists for this sales record");
            }
        }

        oldAIAnalysis.setTopProducts(aiAnalysisIn.getTopProducts());
        oldAIAnalysis.setLowProducts(aiAnalysisIn.getLowProducts());
        oldAIAnalysis.setPeakHours(aiAnalysisIn.getPeakHours());
        oldAIAnalysis.setSlowHours(aiAnalysisIn.getSlowHours());
        oldAIAnalysis.setSurplusProducts(aiAnalysisIn.getSurplusProducts());
        oldAIAnalysis.setSeasonalPatterns(aiAnalysisIn.getSeasonalPatterns());
        oldAIAnalysis.setRecommendation(aiAnalysisIn.getRecommendation());
        oldAIAnalysis.setAiSummary(aiAnalysisIn.getAiSummary());
        oldAIAnalysis.setAnalyzedAt(LocalDateTime.now());
        oldAIAnalysis.setSalesRecord(salesRecord);

        aiAnalysisRepository.save(oldAIAnalysis);
    }

    public void deleteAIAnalysis(Integer id) {
        AIAnalysis aiAnalysis = aiAnalysisRepository.findAIAnalysisById(id);

        if (aiAnalysis == null) {
            throw new ApiException("AI analysis not found");
        }

        if (aiAnalysis.getCampaignSuggestions() != null && !aiAnalysis.getCampaignSuggestions().isEmpty()) {
            throw new ApiException("Cannot delete AI analysis because it has campaign suggestions");
        }

        aiAnalysisRepository.delete(aiAnalysis);
    }

    private void validateAIAnalysisIn(AIAnalysisIn aiAnalysisIn) {
        if (aiAnalysisIn.getTopProducts() == null || aiAnalysisIn.getTopProducts().isBlank()) {
            throw new ApiException("Top products is required");
        }

        if (aiAnalysisIn.getLowProducts() == null || aiAnalysisIn.getLowProducts().isBlank()) {
            throw new ApiException("Low products is required");
        }

        if (aiAnalysisIn.getPeakHours() == null || aiAnalysisIn.getPeakHours().isBlank()) {
            throw new ApiException("Peak hours is required");
        }

        if (aiAnalysisIn.getSlowHours() == null || aiAnalysisIn.getSlowHours().isBlank()) {
            throw new ApiException("Slow hours is required");
        }

        if (aiAnalysisIn.getSurplusProducts() == null || aiAnalysisIn.getSurplusProducts().isBlank()) {
            throw new ApiException("Surplus products is required");
        }

        if (aiAnalysisIn.getRecommendation() == null || aiAnalysisIn.getRecommendation().isBlank()) {
            throw new ApiException("Recommendation is required");
        }

        if (aiAnalysisIn.getAiSummary() == null || aiAnalysisIn.getAiSummary().isBlank()) {
            throw new ApiException("AI summary is required");
        }

        if (aiAnalysisIn.getSalesRecordId() == null) {
            throw new ApiException("Sales record id is required");
        }
    }

    private AIAnalysisOut convertToOut(AIAnalysis aiAnalysis) {
        Integer campaignSuggestionsCount = 0;

        if (aiAnalysis.getCampaignSuggestions() != null) {
            campaignSuggestionsCount = aiAnalysis.getCampaignSuggestions().size();
        }

        return new AIAnalysisOut(
                aiAnalysis.getId(),
                aiAnalysis.getTopProducts(),
                aiAnalysis.getLowProducts(),
                aiAnalysis.getPeakHours(),
                aiAnalysis.getSlowHours(),
                aiAnalysis.getSurplusProducts(),
                aiAnalysis.getSeasonalPatterns(),
                aiAnalysis.getRecommendation(),
                aiAnalysis.getAiSummary(),
                aiAnalysis.getAnalyzedAt(),
                aiAnalysis.getSalesRecord().getId(),
                campaignSuggestionsCount
        );
    }
}
