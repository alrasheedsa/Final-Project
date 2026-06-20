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
    private final EmailService emailService;

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

    public void addAIAnalysis(Integer salesRecordId, AIAnalysisIn aiAnalysisIn) {
        validateAIAnalysisIn(aiAnalysisIn);

        SalesRecord salesRecord = salesRecordRepository.findSalesRecordById(salesRecordId);

        if (salesRecord == null) {
            throw new ApiException("Sales record not found");
        }

        Boolean exists = aiAnalysisRepository.existsBySalesRecord_Id(salesRecordId);

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

        AIAnalysis savedAnalysis = aiAnalysisRepository.save(aiAnalysis);

        sendAIAnalysisEmailSafely(savedAnalysis);
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
            summary.append("\nSales data sample for reference only:\n");
            summary.append(limitRawSalesData(rawSalesData, 1200));
        }

        return summary.toString();
    }

    private String limitRawSalesData(String rawSalesData, Integer maxLength) {
        if (rawSalesData == null || rawSalesData.isBlank()) {
            return "Not available";
        }

        if (rawSalesData.length() <= maxLength) {
            return rawSalesData;
        }

        return rawSalesData.substring(0, maxLength)
                + "\n... Data was shortened to keep AI analysis focused on summarized business metrics.";
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

    public void updateAIAnalysis(Integer id, Integer salesRecordId, AIAnalysisIn aiAnalysisIn) {
        validateAIAnalysisIn(aiAnalysisIn);

        AIAnalysis oldAIAnalysis = aiAnalysisRepository.findAIAnalysisById(id);

        if (oldAIAnalysis == null) {
            throw new ApiException("AI analysis not found");
        }

        SalesRecord salesRecord = salesRecordRepository.findSalesRecordById(salesRecordId);

        if (salesRecord == null) {
            throw new ApiException("Sales record not found");
        }

        Boolean changedSalesRecord =
                !oldAIAnalysis.getSalesRecord().getId().equals(salesRecordId);

        if (changedSalesRecord) {
            Boolean exists = aiAnalysisRepository.existsBySalesRecord_Id(salesRecordId);

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

    public String getPeakHours(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);
        return aiAnalysis.getPeakHours();
    }

    public String getSlowHours(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);
        return aiAnalysis.getSlowHours();
    }

    public String getConfidence(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);
        List<SalesRecordItem> items = getAnalysisItems(aiAnalysis);

        if (items.size() >= 20) {
            return "95%";
        }

        if (items.size() >= 10) {
            return "88%";
        }

        if (items.size() >= 5) {
            return "75%";
        }

        return "60%";
    }

    public List<Map<String, Object>> getSalesChart(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);
        List<SalesRecordItem> items = getAnalysisItems(aiAnalysis);

        Map<Integer, Double> salesByHour = new java.util.TreeMap<>();

        for (SalesRecordItem item : items) {
            if (item.getSaleTime() != null) {
                Integer hour = item.getSaleTime().getHour();

                Double oldTotal = salesByHour.get(hour);
                if (oldTotal == null) {
                    oldTotal = 0.0;
                }

                salesByHour.put(hour, oldTotal + item.getTotalPrice());
            }
        }

        List<Map<String, Object>> chart = new ArrayList<>();

        for (Integer hour : salesByHour.keySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("hour", formatHour(hour));
            row.put("totalSales", salesByHour.get(hour));
            chart.add(row);
        }

        return chart;
    }

    public String getRecommendations(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);
        return aiAnalysis.getRecommendation();
    }

    public String getTopProducts(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);
        return aiAnalysis.getTopProducts();
    }

    public String getLowProducts(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);
        return aiAnalysis.getLowProducts();
    }

    public String getBestRecommendation(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);

        if (aiAnalysis.getRecommendation() != null && !aiAnalysis.getRecommendation().isBlank()) {
            return aiAnalysis.getRecommendation();
        }

        return aiAnalysis.getAiSummary();
    }

    public Double getTotalSales(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);
        List<SalesRecordItem> items = getAnalysisItems(aiAnalysis);

        Double totalSales = 0.0;

        for (SalesRecordItem item : items) {
            if (item.getTotalPrice() != null) {
                totalSales = totalSales + item.getTotalPrice();
            }
        }

        return totalSales;
    }

    public List<Map<String, Object>> getProductDetails(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);
        List<SalesRecordItem> items = getAnalysisItems(aiAnalysis);

        Map<String, Integer> quantityByProduct = new HashMap<>();
        Map<String, Double> salesByProduct = new HashMap<>();

        for (SalesRecordItem item : items) {
            String productName = item.getProductName();

            if (productName == null || productName.isBlank()) {
                continue;
            }

            Integer quantity = item.getQuantity();
            if (quantity == null) {
                quantity = 0;
            }

            Double totalPrice = item.getTotalPrice();
            if (totalPrice == null) {
                totalPrice = 0.0;
            }

            Integer oldQuantity = quantityByProduct.get(productName);
            if (oldQuantity == null) {
                oldQuantity = 0;
            }

            Double oldSales = salesByProduct.get(productName);
            if (oldSales == null) {
                oldSales = 0.0;
            }

            quantityByProduct.put(productName, oldQuantity + quantity);
            salesByProduct.put(productName, oldSales + totalPrice);
        }

        List<Map<String, Object>> productDetails = new ArrayList<>();

        for (String productName : quantityByProduct.keySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("productName", productName);
            row.put("quantity", quantityByProduct.get(productName));
            row.put("totalSales", salesByProduct.get(productName));

            productDetails.add(row);
        }

        return productDetails;
    }

    private AIAnalysis getAIAnalysisEntity(Integer analysisId) {
        AIAnalysis aiAnalysis = aiAnalysisRepository.findAIAnalysisById(analysisId);

        if (aiAnalysis == null) {
            throw new ApiException("AI analysis not found");
        }

        return aiAnalysis;
    }

    private List<SalesRecordItem> getAnalysisItems(AIAnalysis aiAnalysis) {
        List<SalesRecordItem> items =
                salesRecordItemRepository.findAllBySalesRecord_Id(aiAnalysis.getSalesRecord().getId());

        if (items == null || items.isEmpty()) {
            throw new ApiException("Sales record items not found for this analysis");
        }

        return items;
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "غير متوفر";
        }

        return value;
    }

    public String getAnalysisSummary(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);

        return "تحليل مبيعات فرع " + getAnalysisBranchName(analysisId)
                + " لشهر " + aiAnalysis.getSalesRecord().getMonth()
                + " سنة " + aiAnalysis.getSalesRecord().getYear()
                + ". " + safeText(aiAnalysis.getAiSummary());
    }

    public String getSurplusProducts(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);
        return aiAnalysis.getSurplusProducts();
    }

    public String getSeasonalPatterns(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);

        if (aiAnalysis.getSeasonalPatterns() == null || aiAnalysis.getSeasonalPatterns().isBlank()) {
            return "لا توجد أنماط موسمية واضحة في سجل المبيعات الحالي";
        }

        return aiAnalysis.getSeasonalPatterns();
    }

    public String getAiSummary(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);
        return aiAnalysis.getAiSummary();
    }

    public Boolean isSuggestedCampaignReady(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);

        if (aiAnalysis.getRecommendation() == null || aiAnalysis.getRecommendation().isBlank()) {
            return false;
        }

        if (aiAnalysis.getSlowHours() == null || aiAnalysis.getSlowHours().isBlank()) {
            return false;
        }

        if (aiAnalysis.getLowProducts() == null || aiAnalysis.getLowProducts().isBlank()) {
            return false;
        }

        return true;
    }

    public LocalDateTime getAnalysisGeneratedAt(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);
        return aiAnalysis.getAnalyzedAt();
    }

    public String getAnalysisBranchName(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);

        if (aiAnalysis.getSalesRecord() == null
                || aiAnalysis.getSalesRecord().getBranch() == null) {
            throw new ApiException("Branch not found for this analysis");
        }

        return aiAnalysis.getSalesRecord().getBranch().getName();
    }

    public Map<String, Object> getAnalysisSalesRecordInfo(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);

        if (aiAnalysis.getSalesRecord() == null) {
            throw new ApiException("Sales record not found for this analysis");
        }

        Map<String, Object> info = new HashMap<>();

        info.put("salesRecordId", aiAnalysis.getSalesRecord().getId());
        info.put("fileName", aiAnalysis.getSalesRecord().getFileName());
        info.put("month", aiAnalysis.getSalesRecord().getMonth());
        info.put("year", aiAnalysis.getSalesRecord().getYear());
        info.put("uploadedAt", aiAnalysis.getSalesRecord().getUploadedAt());
        info.put("branchName", getAnalysisBranchName(analysisId));

        return info;
    }

    public String getAnalysisMainOpportunity(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);

        return "أفضل فرصة حالية هي استهداف فترة الركود: "
                + safeText(aiAnalysis.getSlowHours())
                + " مع التركيز على المنتجات الأقل مبيعًا: "
                + safeText(aiAnalysis.getLowProducts());
    }

    public String getAnalysisRiskNote(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);
        Double totalSales = getTotalSales(analysisId);

        if (totalSales < 1000) {
            return "تنبيه: إجمالي المبيعات منخفض، لذلك يفضّل استخدام حملة خفيفة التكلفة ومحدودة الوقت";
        }

        if (aiAnalysis.getSlowHours() == null || aiAnalysis.getSlowHours().isBlank()) {
            return "تنبيه: لم يتم اكتشاف وقت ركود واضح، لذلك يفضّل مراجعة بيانات المبيعات قبل إطلاق حملة كبيرة";
        }

        return "لا توجد مخاطر عالية واضحة، لكن يفضّل متابعة أداء الحملة بعد الإطلاق";
    }

    public AIAnalysisOut getLatestAIAnalysisByBranch(Integer branchId) {
        AIAnalysis aiAnalysis =
                aiAnalysisRepository.findFirstBySalesRecord_Branch_IdOrderByAnalyzedAtDesc(branchId);

        if (aiAnalysis == null) {
            throw new ApiException("No AI analysis found for this branch");
        }

        return convertToOut(aiAnalysis);
    }

    public Map<String, Object> getAIAnalysisDashboard(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);

        Map<String, Object> dashboard = new HashMap<>();

        dashboard.put("analysisId", aiAnalysis.getId());
        dashboard.put("branchName", getAnalysisBranchName(analysisId));
        dashboard.put("salesRecordInfo", getAnalysisSalesRecordInfo(analysisId));
        dashboard.put("summary", getAnalysisSummary(analysisId));
        dashboard.put("aiSummary", getAiSummary(analysisId));

        dashboard.put("totalSales", getTotalSales(analysisId));
        dashboard.put("peakHours", getPeakHours(analysisId));
        dashboard.put("slowHours", getSlowHours(analysisId));
        dashboard.put("confidence", getConfidence(analysisId));

        dashboard.put("salesChart", getSalesChart(analysisId));
        dashboard.put("topProducts", getTopProducts(analysisId));
        dashboard.put("lowProducts", getLowProducts(analysisId));
        dashboard.put("productDetails", getProductDetails(analysisId));

        dashboard.put("surplusProducts", getSurplusProducts(analysisId));
        dashboard.put("seasonalPatterns", getSeasonalPatterns(analysisId));
        dashboard.put("recommendation", getRecommendations(analysisId));
        dashboard.put("bestRecommendation", getBestRecommendation(analysisId));

        dashboard.put("suggestedCampaignReady", isSuggestedCampaignReady(analysisId));
        dashboard.put("mainOpportunity", getAnalysisMainOpportunity(analysisId));
        dashboard.put("riskNote", getAnalysisRiskNote(analysisId));
        dashboard.put("generatedAt", getAnalysisGeneratedAt(analysisId));

        return dashboard;
    }

    public String sendAIAnalysisSummaryEmail(Integer analysisId) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(analysisId);

        return sendAIAnalysisEmail(aiAnalysis);
    }

    private void sendAIAnalysisEmailSafely(AIAnalysis aiAnalysis) {
        try {
            sendAIAnalysisEmail(aiAnalysis);
        } catch (Exception e) {
            System.out.println("AI analysis email was not sent: " + e.getMessage());
        }
    }

    private String sendAIAnalysisEmail(AIAnalysis aiAnalysis) {
        if (aiAnalysis.getSalesRecord() == null
                || aiAnalysis.getSalesRecord().getBranch() == null
                || aiAnalysis.getSalesRecord().getBranch().getStore() == null
                || aiAnalysis.getSalesRecord().getBranch().getStore().getStoreOwner() == null
                || aiAnalysis.getSalesRecord().getBranch().getStore().getStoreOwner().getUser() == null) {
            throw new ApiException("Store owner email information not found");
        }

        String ownerEmail =
                aiAnalysis.getSalesRecord()
                        .getBranch()
                        .getStore()
                        .getStoreOwner()
                        .getUser()
                        .getEmail();

        String ownerName =
                aiAnalysis.getSalesRecord()
                        .getBranch()
                        .getStore()
                        .getStoreOwner()
                        .getUser()
                        .getFullName();

        String branchName = aiAnalysis.getSalesRecord().getBranch().getName();

        return emailService.sendAIAnalysisReadyEmail(
                ownerEmail,
                ownerName,
                branchName,
                aiAnalysis.getSalesRecord().getMonth(),
                aiAnalysis.getSalesRecord().getYear(),
                aiAnalysis.getSlowHours(),
                aiAnalysis.getPeakHours(),
                aiAnalysis.getTopProducts(),
                aiAnalysis.getLowProducts(),
                aiAnalysis.getRecommendation()
        );
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
