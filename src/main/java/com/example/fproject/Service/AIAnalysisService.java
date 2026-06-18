package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.AIAnalysisIn;
import com.example.fproject.DTO.OUT.AIAnalysisOut;
import com.example.fproject.Model.AIAnalysis;
import com.example.fproject.Model.SalesRecord;
import com.example.fproject.Model.SalesRecordItem;
import com.example.fproject.Repository.AIAnalysisRepository;
import com.example.fproject.Repository.SalesRecordItemRepository;
import com.example.fproject.Repository.SalesRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIAnalysisService {

    private final AIAnalysisRepository aiAnalysisRepository;
    private final SalesRecordRepository salesRecordRepository;
    private final SalesRecordItemRepository salesRecordItemRepository;
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

        Boolean exists = aiAnalysisRepository.existsBySalesRecord_Id(salesRecordId);

        if (Boolean.TRUE.equals(exists)) {
            throw new ApiException("This sales record already has AI analysis");
        }

        String salesSummary = buildSalesSummaryForAI(salesRecord, salesData);

        OpenAiService.AIAnalysisResult result = openAiService.analyzeSalesDataForAIAnalysis(salesSummary);

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

    private String buildSalesSummaryForAI(SalesRecord salesRecord, String rawSalesData) {
        List<SalesRecordItem> items = salesRecordItemRepository.findAllBySalesRecord_Id(salesRecord.getId());

        if (items == null || items.isEmpty()) {
            throw new ApiException("Sales record items are required before AI analysis");
        }

        Map<String, Integer> quantityByProduct = new HashMap<>();
        Map<String, Double> revenueByProduct = new HashMap<>();
        Map<Integer, Integer> quantityByHour = new HashMap<>();
        Map<Integer, Double> revenueByHour = new HashMap<>();

        Integer totalQuantity = 0;
        Double totalRevenue = 0.0;

        for (SalesRecordItem item : items) {
            String productName = item.getProductName();
            Integer quantity = item.getQuantity();
            Double unitPrice = item.getUnitPrice();
            Double totalPrice = item.getTotalPrice();

            if (totalPrice == null && quantity != null && unitPrice != null) {
                totalPrice = quantity * unitPrice;
            }

            if (productName == null || productName.isBlank()) {
                continue;
            }

            if (quantity == null) {
                quantity = 0;
            }

            if (totalPrice == null) {
                totalPrice = 0.0;
            }

            totalQuantity = totalQuantity + quantity;
            totalRevenue = totalRevenue + totalPrice;

            Integer oldProductQuantity = quantityByProduct.get(productName);
            if (oldProductQuantity == null) {
                oldProductQuantity = 0;
            }
            quantityByProduct.put(productName, oldProductQuantity + quantity);

            Double oldProductRevenue = revenueByProduct.get(productName);
            if (oldProductRevenue == null) {
                oldProductRevenue = 0.0;
            }
            revenueByProduct.put(productName, oldProductRevenue + totalPrice);

            if (item.getSaleTime() != null) {
                Integer hour = item.getSaleTime().getHour();

                Integer oldHourQuantity = quantityByHour.get(hour);
                if (oldHourQuantity == null) {
                    oldHourQuantity = 0;
                }
                quantityByHour.put(hour, oldHourQuantity + quantity);

                Double oldHourRevenue = revenueByHour.get(hour);
                if (oldHourRevenue == null) {
                    oldHourRevenue = 0.0;
                }
                revenueByHour.put(hour, oldHourRevenue + totalPrice);
            }
        }

        String topProduct = findTopProduct(quantityByProduct);
        String lowProduct = findLowProduct(quantityByProduct);
        Integer peakHour = findPeakHour(revenueByHour);
        Integer slowHour = findSlowHour(revenueByHour);

        StringBuilder summary = new StringBuilder();

        summary.append("Branch sales record summary for AI analysis:\n");
        summary.append("SalesRecord ID: ").append(salesRecord.getId()).append("\n");
        summary.append("Branch ID: ").append(salesRecord.getBranch().getId()).append("\n");
        summary.append("Month: ").append(salesRecord.getMonth()).append("\n");
        summary.append("Year: ").append(salesRecord.getYear()).append("\n");
        summary.append("Total rows: ").append(items.size()).append("\n");
        summary.append("Total quantity sold: ").append(totalQuantity).append("\n");
        summary.append("Total revenue: ").append(totalRevenue).append("\n");
        summary.append("Top product by quantity: ").append(topProduct).append("\n");
        summary.append("Lowest product by quantity: ").append(lowProduct).append("\n");
        summary.append("Peak sales hour by revenue: ").append(formatHour(peakHour)).append("\n");
        summary.append("Slow sales hour by revenue: ").append(formatHour(slowHour)).append("\n\n");

        summary.append("Product performance:\n");
        for (String productName : quantityByProduct.keySet()) {
            summary.append("- ")
                    .append(productName)
                    .append(": quantity=")
                    .append(quantityByProduct.get(productName))
                    .append(", revenue=")
                    .append(revenueByProduct.get(productName))
                    .append("\n");
        }

        summary.append("\nHourly performance:\n");
        for (Integer hour : revenueByHour.keySet()) {
            summary.append("- ")
                    .append(formatHour(hour))
                    .append(": quantity=")
                    .append(quantityByHour.get(hour))
                    .append(", revenue=")
                    .append(revenueByHour.get(hour))
                    .append("\n");
        }

        if (rawSalesData != null && !rawSalesData.isBlank()) {
            summary.append("\nRaw Excel text for reference:\n");
            summary.append(rawSalesData);
        }

        return summary.toString();
    }

    private String findTopProduct(Map<String, Integer> quantityByProduct) {
        String topProduct = "Not available";
        Integer highestQuantity = -1;

        for (String productName : quantityByProduct.keySet()) {
            Integer quantity = quantityByProduct.get(productName);

            if (quantity > highestQuantity) {
                highestQuantity = quantity;
                topProduct = productName;
            }
        }

        return topProduct;
    }

    private String findLowProduct(Map<String, Integer> quantityByProduct) {
        String lowProduct = "Not available";
        Integer lowestQuantity = null;

        for (String productName : quantityByProduct.keySet()) {
            Integer quantity = quantityByProduct.get(productName);

            if (lowestQuantity == null || quantity < lowestQuantity) {
                lowestQuantity = quantity;
                lowProduct = productName;
            }
        }

        return lowProduct;
    }

    private Integer findPeakHour(Map<Integer, Double> revenueByHour) {
        Integer peakHour = null;
        Double highestRevenue = -1.0;

        for (Integer hour : revenueByHour.keySet()) {
            Double revenue = revenueByHour.get(hour);

            if (revenue > highestRevenue) {
                highestRevenue = revenue;
                peakHour = hour;
            }
        }

        return peakHour;
    }

    private Integer findSlowHour(Map<Integer, Double> revenueByHour) {
        Integer slowHour = null;
        Double lowestRevenue = null;

        for (Integer hour : revenueByHour.keySet()) {
            Double revenue = revenueByHour.get(hour);

            if (lowestRevenue == null || revenue < lowestRevenue) {
                lowestRevenue = revenue;
                slowHour = hour;
            }
        }

        return slowHour;
    }

    private String formatHour(Integer hour) {
        if (hour == null) {
            return "Not available";
        }

        Integer nextHour = hour + 1;

        if (nextHour == 24) {
            nextHour = 0;
        }

        return String.format("%02d:00 - %02d:00", hour, nextHour);
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
