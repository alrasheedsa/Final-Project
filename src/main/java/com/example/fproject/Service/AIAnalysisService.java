package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.AIAnalysisIn;
import com.example.fproject.DTO.OUT.AIAnalysisOut;
import com.example.fproject.Model.AIAnalysis;
import com.example.fproject.Model.SalesRecord;
import com.example.fproject.Model.SalesRecordItem;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Repository.AIAnalysisRepository;
import com.example.fproject.Repository.SalesRecordItemRepository;
import com.example.fproject.Repository.SalesRecordRepository;
import com.example.fproject.Repository.StoreOwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AIAnalysisService {

    private final AIAnalysisRepository aiAnalysisRepository;
    private final SalesRecordRepository salesRecordRepository;
    private final SalesRecordItemRepository salesRecordItemRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final OpenAiService openAiService;
    private final EmailService emailService;

    public List<AIAnalysisOut> getAllAIAnalyses() {
        List<AIAnalysis> list = aiAnalysisRepository.findAll();
        List<AIAnalysisOut> result = new ArrayList<>();
        for (AIAnalysis a : list) result.add(convertToOut(a));
        return result;
    }

    public AIAnalysisOut getAIAnalysisById(Integer userId, Integer id) {
        AIAnalysis aiAnalysis = getAIAnalysisEntity(id);
        verifyOwnership(userId, aiAnalysis);
        return convertToOut(aiAnalysis);
    }

    public AIAnalysisOut getAIAnalysisBySalesRecordId(Integer userId, Integer salesRecordId) {
        SalesRecord salesRecord = findSalesRecordOrThrow(salesRecordId);
        verifyOwnershipBySalesRecord(userId, salesRecord);
        AIAnalysis aiAnalysis = aiAnalysisRepository.findAIAnalysisBySalesRecord_Id(salesRecordId);
        if (aiAnalysis == null) throw new ApiException("AI analysis not found for this sales record");
        return convertToOut(aiAnalysis);
    }

    public void addAIAnalysis(Integer userId, Integer salesRecordId, AIAnalysisIn aiAnalysisIn) {
        validateAIAnalysisIn(aiAnalysisIn);
        SalesRecord salesRecord = findSalesRecordOrThrow(salesRecordId);
        verifyOwnershipBySalesRecord(userId, salesRecord);

        if (Boolean.TRUE.equals(aiAnalysisRepository.existsBySalesRecord_Id(salesRecordId)))
            throw new ApiException("This sales record already has AI analysis");

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

    // استخدام داخلي من SalesRecordService — بدون تحقق من الـ userId
    public void generateAIAnalysisFromSalesRecord(Integer salesRecordId, String salesData) {
        SalesRecord salesRecord = findSalesRecordOrThrow(salesRecordId);

        if (Boolean.TRUE.equals(aiAnalysisRepository.existsBySalesRecord_Id(salesRecordId)))
            throw new ApiException("This sales record already has AI analysis");

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

        AIAnalysis saved = aiAnalysisRepository.save(aiAnalysis);
        sendAIAnalysisEmailSafely(saved);
    }

    public void updateAIAnalysis(Integer userId, Integer id, Integer salesRecordId, AIAnalysisIn aiAnalysisIn) {
        validateAIAnalysisIn(aiAnalysisIn);
        AIAnalysis old = getAIAnalysisEntity(id);
        verifyOwnership(userId, old);

        SalesRecord salesRecord = findSalesRecordOrThrow(salesRecordId);
        verifyOwnershipBySalesRecord(userId, salesRecord);

        if (!old.getSalesRecord().getId().equals(salesRecordId)) {
            if (Boolean.TRUE.equals(aiAnalysisRepository.existsBySalesRecord_Id(salesRecordId)))
                throw new ApiException("Another AI analysis already exists for this sales record");
        }

        old.setTopProducts(aiAnalysisIn.getTopProducts());
        old.setLowProducts(aiAnalysisIn.getLowProducts());
        old.setPeakHours(aiAnalysisIn.getPeakHours());
        old.setSlowHours(aiAnalysisIn.getSlowHours());
        old.setSurplusProducts(aiAnalysisIn.getSurplusProducts());
        old.setSeasonalPatterns(aiAnalysisIn.getSeasonalPatterns());
        old.setRecommendation(aiAnalysisIn.getRecommendation());
        old.setAiSummary(aiAnalysisIn.getAiSummary());
        old.setAnalyzedAt(LocalDateTime.now());
        old.setSalesRecord(salesRecord);
        aiAnalysisRepository.save(old);
    }

    public String getPeakHours(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        return a.getPeakHours();
    }

    public String getSlowHours(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        return a.getSlowHours();
    }

    public String getConfidence(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        List<SalesRecordItem> items = getAnalysisItems(a);
        if (items.size() >= 20) return "95%";
        if (items.size() >= 10) return "88%";
        if (items.size() >= 5)  return "75%";
        return "60%";
    }

    public List<Map<String, Object>> getSalesChart(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        List<SalesRecordItem> items = getAnalysisItems(a);
        Map<Integer, Double> salesByHour = new java.util.TreeMap<>();
        for (SalesRecordItem item : items) {
            if (item.getSaleTime() != null) {
                Integer hour = item.getSaleTime().getHour();
                salesByHour.merge(hour, item.getTotalPrice(), Double::sum);
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

    public String getRecommendations(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        return a.getRecommendation();
    }

    public String getTopProducts(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        return a.getTopProducts();
    }

    public String getLowProducts(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        return a.getLowProducts();
    }

    public String getBestRecommendation(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        if (a.getRecommendation() != null && !a.getRecommendation().isBlank()) return a.getRecommendation();
        return a.getAiSummary();
    }

    public Double getTotalSales(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        return calcTotalSales(a);
    }

    public List<Map<String, Object>> getProductDetails(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        return calcProductDetails(a);
    }

    public String getAnalysisSummary(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        return "تحليل مبيعات فرع " + a.getSalesRecord().getBranch().getName()
                + " لشهر " + a.getSalesRecord().getMonth()
                + " سنة " + a.getSalesRecord().getYear()
                + ". " + safeText(a.getAiSummary());
    }

    public String getSurplusProducts(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        return a.getSurplusProducts();
    }

    public String getSeasonalPatterns(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        if (a.getSeasonalPatterns() == null || a.getSeasonalPatterns().isBlank())
            return "لا توجد أنماط موسمية واضحة في سجل المبيعات الحالي";
        return a.getSeasonalPatterns();
    }

    public String getAiSummary(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        return a.getAiSummary();
    }

    public Boolean isSuggestedCampaignReady(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        return a.getRecommendation() != null && !a.getRecommendation().isBlank()
                && a.getSlowHours() != null && !a.getSlowHours().isBlank()
                && a.getLowProducts() != null && !a.getLowProducts().isBlank();
    }

    public LocalDateTime getAnalysisGeneratedAt(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        return a.getAnalyzedAt();
    }

    public String getAnalysisBranchName(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        if (a.getSalesRecord() == null || a.getSalesRecord().getBranch() == null)
            throw new ApiException("Branch not found for this analysis");
        return a.getSalesRecord().getBranch().getName();
    }

    public Map<String, Object> getAnalysisSalesRecordInfo(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        if (a.getSalesRecord() == null) throw new ApiException("Sales record not found for this analysis");
        Map<String, Object> info = new HashMap<>();
        info.put("salesRecordId", a.getSalesRecord().getId());
        info.put("fileName", a.getSalesRecord().getFileName());
        info.put("month", a.getSalesRecord().getMonth());
        info.put("year", a.getSalesRecord().getYear());
        info.put("uploadedAt", a.getSalesRecord().getUploadedAt());
        info.put("branchName", a.getSalesRecord().getBranch().getName());
        return info;
    }

    public String getAnalysisMainOpportunity(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        return "أفضل فرصة حالية هي استهداف فترة الركود: " + safeText(a.getSlowHours())
                + " مع التركيز على المنتجات الأقل مبيعًا: " + safeText(a.getLowProducts());
    }

    public String getAnalysisRiskNote(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        Double totalSales = calcTotalSales(a);
        if (totalSales < 1000) return "تنبيه: إجمالي المبيعات منخفض، لذلك يفضّل استخدام حملة خفيفة التكلفة ومحدودة الوقت";
        if (a.getSlowHours() == null || a.getSlowHours().isBlank())
            return "تنبيه: لم يتم اكتشاف وقت ركود واضح، لذلك يفضّل مراجعة بيانات المبيعات قبل إطلاق حملة كبيرة";
        return "لا توجد مخاطر عالية واضحة، لكن يفضّل متابعة أداء الحملة بعد الإطلاق";
    }

    public AIAnalysisOut getLatestAIAnalysisByBranch(Integer userId, Integer branchId) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null) throw new ApiException("Store owner not found");
        AIAnalysis aiAnalysis = aiAnalysisRepository.findFirstBySalesRecord_Branch_IdOrderByAnalyzedAtDesc(branchId);
        if (aiAnalysis == null) throw new ApiException("No AI analysis found for this branch");
        verifyOwnership(userId, aiAnalysis);
        return convertToOut(aiAnalysis);
    }

    public Map<String, Object> getAIAnalysisDashboard(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("analysisId", a.getId());
        dashboard.put("branchName", getAnalysisBranchName(userId, analysisId));
        dashboard.put("salesRecordInfo", getAnalysisSalesRecordInfo(userId, analysisId));
        dashboard.put("summary", getAnalysisSummary(userId, analysisId));
        dashboard.put("aiSummary", getAiSummary(userId, analysisId));
        dashboard.put("totalSales", getTotalSales(userId, analysisId));
        dashboard.put("peakHours", getPeakHours(userId, analysisId));
        dashboard.put("slowHours", getSlowHours(userId, analysisId));
        dashboard.put("confidence", getConfidence(userId, analysisId));
        dashboard.put("salesChart", getSalesChart(userId, analysisId));
        dashboard.put("topProducts", getTopProducts(userId, analysisId));
        dashboard.put("lowProducts", getLowProducts(userId, analysisId));
        dashboard.put("productDetails", getProductDetails(userId, analysisId));
        dashboard.put("surplusProducts", getSurplusProducts(userId, analysisId));
        dashboard.put("seasonalPatterns", getSeasonalPatterns(userId, analysisId));
        dashboard.put("recommendation", getRecommendations(userId, analysisId));
        dashboard.put("bestRecommendation", getBestRecommendation(userId, analysisId));
        dashboard.put("suggestedCampaignReady", isSuggestedCampaignReady(userId, analysisId));
        dashboard.put("mainOpportunity", getAnalysisMainOpportunity(userId, analysisId));
        dashboard.put("riskNote", getAnalysisRiskNote(userId, analysisId));
        dashboard.put("generatedAt", getAnalysisGeneratedAt(userId, analysisId));
        return dashboard;
    }

    public String sendAIAnalysisSummaryEmail(Integer userId, Integer analysisId) {
        AIAnalysis a = getAIAnalysisEntity(analysisId);
        verifyOwnership(userId, a);
        return sendAIAnalysisEmail(a);
    }

    public void deleteAIAnalysis(Integer userId, Integer id) {
        AIAnalysis a = getAIAnalysisEntity(id);
        verifyOwnership(userId, a);
        if (a.getCampaignSuggestions() != null && !a.getCampaignSuggestions().isEmpty())
            throw new ApiException("Cannot delete AI analysis because it has campaign suggestions");
        aiAnalysisRepository.delete(a);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void verifyOwnership(Integer userId, AIAnalysis aiAnalysis) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null || !aiAnalysis.getSalesRecord().getBranch()
                .getStore().getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("You do not have permission to access this resource");
    }

    private void verifyOwnershipBySalesRecord(Integer userId, SalesRecord salesRecord) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null || !salesRecord.getBranch().getStore().getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("You do not have permission to access this resource");
    }

    private AIAnalysis getAIAnalysisEntity(Integer analysisId) {
        AIAnalysis a = aiAnalysisRepository.findAIAnalysisById(analysisId);
        if (a == null) throw new ApiException("AI analysis not found");
        return a;
    }

    private SalesRecord findSalesRecordOrThrow(Integer salesRecordId) {
        SalesRecord sr = salesRecordRepository.findSalesRecordById(salesRecordId);
        if (sr == null) throw new ApiException("Sales record not found");
        return sr;
    }

    private List<SalesRecordItem> getAnalysisItems(AIAnalysis aiAnalysis) {
        List<SalesRecordItem> items =
                salesRecordItemRepository.findAllBySalesRecord_Id(aiAnalysis.getSalesRecord().getId());
        if (items == null || items.isEmpty()) throw new ApiException("Sales record items not found for this analysis");
        return items;
    }

    private Double calcTotalSales(AIAnalysis aiAnalysis) {
        List<SalesRecordItem> items = getAnalysisItems(aiAnalysis);
        double total = 0.0;
        for (SalesRecordItem item : items) {
            if (item.getTotalPrice() != null) total += item.getTotalPrice();
        }
        return total;
    }

    private List<Map<String, Object>> calcProductDetails(AIAnalysis aiAnalysis) {
        List<SalesRecordItem> items = getAnalysisItems(aiAnalysis);
        Map<String, Integer> qtyMap = new HashMap<>();
        Map<String, Double> salesMap = new HashMap<>();
        for (SalesRecordItem item : items) {
            String name = item.getProductName();
            if (name == null || name.isBlank()) continue;
            qtyMap.merge(name, item.getQuantity() != null ? item.getQuantity() : 0, Integer::sum);
            salesMap.merge(name, item.getTotalPrice() != null ? item.getTotalPrice() : 0.0, Double::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String name : qtyMap.keySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("productName", name);
            row.put("quantity", qtyMap.get(name));
            row.put("totalSales", salesMap.get(name));
            result.add(row);
        }
        return result;
    }

    private String safeText(String value) {
        return (value == null || value.isBlank()) ? "غير متوفر" : value;
    }

    private String formatHour(Integer hour) {
        if (hour == null) return "Not available";
        int next = (hour + 1) % 24;
        return String.format("%02d:00 - %02d:00", hour, next);
    }

    private String buildSalesSummaryForAI(SalesRecord salesRecord, String rawSalesData) {
        List<SalesRecordItem> items = salesRecordItemRepository.findAllBySalesRecord_Id(salesRecord.getId());
        if (items == null || items.isEmpty())
            throw new ApiException("Sales record items are required before AI analysis");

        Map<String, Integer> quantityByProduct = new HashMap<>();
        Map<String, Double> revenueByProduct   = new HashMap<>();
        Map<Integer, Integer> quantityByHour   = new HashMap<>();
        Map<Integer, Double> revenueByHour     = new HashMap<>();
        Integer totalQuantity = 0;
        Double totalRevenue   = 0.0;

        for (SalesRecordItem item : items) {
            String name = item.getProductName();
            Integer qty = item.getQuantity() != null ? item.getQuantity() : 0;
            Double total = item.getTotalPrice() != null ? item.getTotalPrice()
                    : (item.getUnitPrice() != null ? qty * item.getUnitPrice() : 0.0);
            if (name == null || name.isBlank()) continue;
            totalQuantity += qty;
            totalRevenue  += total;
            quantityByProduct.merge(name, qty, Integer::sum);
            revenueByProduct.merge(name, total, Double::sum);
            if (item.getSaleTime() != null) {
                int hour = item.getSaleTime().getHour();
                quantityByHour.merge(hour, qty, Integer::sum);
                revenueByHour.merge(hour, total, Double::sum);
            }
        }

        StringBuilder sb = new StringBuilder("Branch sales record summary for AI analysis:\n");
        sb.append("SalesRecord ID: ").append(salesRecord.getId()).append("\n");
        sb.append("Branch ID: ").append(salesRecord.getBranch().getId()).append("\n");
        sb.append("Month: ").append(salesRecord.getMonth()).append("\n");
        sb.append("Year: ").append(salesRecord.getYear()).append("\n");
        sb.append("Total rows: ").append(items.size()).append("\n");
        sb.append("Total quantity sold: ").append(totalQuantity).append("\n");
        sb.append("Total revenue: ").append(totalRevenue).append("\n");

        sb.append("\nProduct performance:\n");
        for (String name : quantityByProduct.keySet()) {
            sb.append("- ").append(name)
                    .append(": quantity=").append(quantityByProduct.get(name))
                    .append(", revenue=").append(revenueByProduct.get(name)).append("\n");
        }
        sb.append("\nHourly performance:\n");
        for (Integer hour : revenueByHour.keySet()) {
            sb.append("- ").append(formatHour(hour))
                    .append(": quantity=").append(quantityByHour.get(hour))
                    .append(", revenue=").append(revenueByHour.get(hour)).append("\n");
        }
        if (rawSalesData != null && !rawSalesData.isBlank()) {
            sb.append("\nSales data sample:\n");
            sb.append(rawSalesData.length() <= 1200 ? rawSalesData
                    : rawSalesData.substring(0, 1200) + "\n... shortened.");
        }
        return sb.toString();
    }

    private void sendAIAnalysisEmailSafely(AIAnalysis aiAnalysis) {
        try { sendAIAnalysisEmail(aiAnalysis); }
        catch (Exception e) { System.out.println("AI analysis email not sent: " + e.getMessage()); }
    }

    private String sendAIAnalysisEmail(AIAnalysis aiAnalysis) {
        if (aiAnalysis.getSalesRecord() == null
                || aiAnalysis.getSalesRecord().getBranch() == null
                || aiAnalysis.getSalesRecord().getBranch().getStore() == null
                || aiAnalysis.getSalesRecord().getBranch().getStore().getStoreOwner() == null
                || aiAnalysis.getSalesRecord().getBranch().getStore().getStoreOwner().getUser() == null)
            throw new ApiException("Store owner email information not found");

        String ownerEmail = aiAnalysis.getSalesRecord().getBranch().getStore().getStoreOwner().getUser().getEmail();
        String ownerName  = aiAnalysis.getSalesRecord().getBranch().getStore().getStoreOwner().getUser().getFullName();
        String branchName = aiAnalysis.getSalesRecord().getBranch().getName();

        return emailService.sendAIAnalysisReadyEmail(
                ownerEmail, ownerName, branchName,
                aiAnalysis.getSalesRecord().getMonth(),
                aiAnalysis.getSalesRecord().getYear(),
                aiAnalysis.getSlowHours(), aiAnalysis.getPeakHours(),
                aiAnalysis.getTopProducts(), aiAnalysis.getLowProducts(),
                aiAnalysis.getRecommendation()
        );
    }

    private void validateAIAnalysisIn(AIAnalysisIn in) {
        if (in.getTopProducts() == null || in.getTopProducts().isBlank()) throw new ApiException("Top products is required");
        if (in.getLowProducts() == null || in.getLowProducts().isBlank()) throw new ApiException("Low products is required");
        if (in.getPeakHours() == null || in.getPeakHours().isBlank()) throw new ApiException("Peak hours is required");
        if (in.getSlowHours() == null || in.getSlowHours().isBlank()) throw new ApiException("Slow hours is required");
        if (in.getSurplusProducts() == null || in.getSurplusProducts().isBlank()) throw new ApiException("Surplus products is required");
        if (in.getRecommendation() == null || in.getRecommendation().isBlank()) throw new ApiException("Recommendation is required");
        if (in.getAiSummary() == null || in.getAiSummary().isBlank()) throw new ApiException("AI summary is required");
    }

    private AIAnalysisOut convertToOut(AIAnalysis a) {
        int count = a.getCampaignSuggestions() != null ? a.getCampaignSuggestions().size() : 0;
        return new AIAnalysisOut(
                a.getId(), a.getTopProducts(), a.getLowProducts(),
                a.getPeakHours(), a.getSlowHours(), a.getSurplusProducts(),
                a.getSeasonalPatterns(), a.getRecommendation(), a.getAiSummary(),
                a.getAnalyzedAt(), a.getSalesRecord().getId(), count
        );
    }
}