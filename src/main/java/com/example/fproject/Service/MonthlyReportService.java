package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.MonthlyReportIn;
import com.example.fproject.DTO.OUT.MonthlyReportOut;
import com.example.fproject.Model.Branch;
import com.example.fproject.Model.MonthlyReport;
import com.example.fproject.Model.SalesRecord;
import com.example.fproject.Model.SalesRecordItem;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlyReportService {

    private final MonthlyReportRepository monthlyReportRepository;
    private final BranchRepository branchRepository;
    private final SalesRecordRepository salesRecordRepository;
    private final SalesRecordItemRepository salesRecordItemRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final BranchService branchService;
    private final ITextService iTextService;
    private final OpenAiService openAiService;
    private final EmailService emailService;

    @Transactional
    public MonthlyReportOut generateMonthlyReport(Integer userId, Integer branchId, MonthlyReportIn dto) {
        Branch branch = findBranchOrThrow(branchId);
        verifyOwnership(userId, branch);

        if (!branchService.isBranchSubscribed(branchId))
            throw new ApiException("Branch must have an active subscription to generate a monthly report");

        if (monthlyReportRepository.existsMonthlyReportByBranchIdAndMonthAndYear(branchId, dto.getMonth(), dto.getYear()))
            throw new ApiException("A monthly report already exists for " + monthName(dto.getMonth()) + " " + dto.getYear());

        SalesRecord salesRecord = salesRecordRepository.findByBranch_IdAndMonthAndYear(branchId, dto.getMonth(), dto.getYear());
        if (salesRecord == null)
            throw new ApiException("No sales record found for " + monthName(dto.getMonth()) + " " + dto.getYear());

        List<SalesRecordItem> items = salesRecordItemRepository.findAllBySalesRecord_Id(salesRecord.getId())
                .stream().filter(i -> i.getSaleDate() != null
                        && i.getSaleDate().getMonthValue() == dto.getMonth()
                        && i.getSaleDate().getYear() == dto.getYear()).toList();

        if (items.isEmpty())
            throw new ApiException("No sales items found for " + monthName(dto.getMonth()) + " " + dto.getYear());

        SalesStats stats = calculateStats(items);
        String aiSummary = generateAiSummary(branch, dto, stats);

        MonthlyReport report = new MonthlyReport();
        report.setMonth(dto.getMonth()); report.setYear(dto.getYear());
        report.setTotalSales(stats.totalSales()); report.setTotalQuantity(stats.totalQuantity());
        report.setTopProducts(stats.topProducts()); report.setLowProducts(stats.lowProducts());
        report.setPeakHours(stats.peakHours()); report.setSlowHours(stats.slowHours());
        report.setSurplusProducts(stats.surplusProducts()); report.setAiSummary(aiSummary);
        report.setGeneratedAt(LocalDateTime.now()); report.setBranch(branch);
        report.setPdfUrl("pending");

        monthlyReportRepository.save(report);
        report.setPdfUrl("/api/v1/monthly-report/download/" + report.getId());
        monthlyReportRepository.save(report);
        return mapToOut(report);
    }

    public List<MonthlyReportOut> getAllMonthlyReports() {
        return monthlyReportRepository.findAll().stream().map(this::mapToOut).toList();
    }

    public MonthlyReportOut getMonthlyReportById(Integer userId, Integer reportId) {
        MonthlyReport report = findReportOrThrow(reportId);
        verifyOwnership(userId, report.getBranch());
        return mapToOut(report);
    }

    public List<MonthlyReportOut> getMonthlyReportsByBranchId(Integer userId, Integer branchId) {
        Branch branch = findBranchOrThrow(branchId);
        verifyOwnership(userId, branch);
        return monthlyReportRepository.findMonthlyReportsByBranchIdOrderByYearDescMonthDesc(branchId)
                .stream().map(this::mapToOut).toList();
    }

    public MonthlyReportOut getMonthlyReportByBranchAndDate(Integer userId, Integer branchId, Integer month, Integer year) {
        Branch branch = findBranchOrThrow(branchId);
        verifyOwnership(userId, branch);
        MonthlyReport report = monthlyReportRepository.findMonthlyReportByBranchIdAndMonthAndYear(branchId, month, year);
        if (report == null) throw new ApiException("No report found for " + monthName(month) + " " + year);
        return mapToOut(report);
    }

    @Transactional
    public MonthlyReportOut regenerateMonthlyReport(Integer userId, Integer reportId) {
        MonthlyReport existing = findReportOrThrow(reportId);
        verifyOwnership(userId, existing.getBranch());

        Branch branch   = existing.getBranch();
        Integer branchId = branch.getId();
        Integer month   = existing.getMonth();
        Integer year    = existing.getYear();

        if (!branchService.isBranchSubscribed(branchId))
            throw new ApiException("Branch must have an active subscription to regenerate a report");

        SalesRecord salesRecord = salesRecordRepository.findByBranch_IdAndMonthAndYear(branchId, month, year);
        if (salesRecord == null)
            throw new ApiException("No sales record found for " + monthName(month) + " " + year);

        List<SalesRecordItem> items = salesRecordItemRepository.findAllBySalesRecord_Id(salesRecord.getId())
                .stream().filter(i -> i.getSaleDate() != null
                        && i.getSaleDate().getMonthValue() == month
                        && i.getSaleDate().getYear() == year).toList();

        if (items.isEmpty()) throw new ApiException("No sales items found for " + monthName(month) + " " + year);

        SalesStats stats   = calculateStats(items);
        String aiSummary   = generateAiSummary(branch, new MonthlyReportIn(month, year), stats);

        existing.setTotalSales(stats.totalSales()); existing.setTotalQuantity(stats.totalQuantity());
        existing.setTopProducts(stats.topProducts()); existing.setLowProducts(stats.lowProducts());
        existing.setPeakHours(stats.peakHours()); existing.setSlowHours(stats.slowHours());
        existing.setSurplusProducts(stats.surplusProducts()); existing.setAiSummary(aiSummary);
        existing.setGeneratedAt(LocalDateTime.now());
        monthlyReportRepository.save(existing);
        return mapToOut(existing);
    }

    @Transactional
    public void deleteMonthlyReport(Integer userId, Integer reportId) {
        MonthlyReport report = findReportOrThrow(reportId);
        verifyOwnership(userId, report.getBranch());
        monthlyReportRepository.delete(report);
    }

    public byte[] downloadMonthlyReport(Integer userId, Integer reportId) {
        MonthlyReport report = findReportOrThrow(reportId);
        verifyOwnership(userId, report.getBranch());
        return iTextService.generateSalesMonthlyReportPdf(
                report.getBranch().getStore().getName(), report.getBranch().getName(),
                report.getMonth(), report.getYear(), report.getTotalSales(), report.getTotalQuantity(),
                report.getTopProducts(), report.getLowProducts(),
                report.getPeakHours(), report.getSlowHours(), report.getAiSummary()
        );
    }

    public void sendReportByEmail(Integer userId, Integer reportId, String toEmail) {
        MonthlyReport report = findReportOrThrow(reportId);
        verifyOwnership(userId, report.getBranch());

        byte[] pdf = iTextService.generateSalesMonthlyReportPdf(
                report.getBranch().getStore().getName(), report.getBranch().getName(),
                report.getMonth(), report.getYear(), report.getTotalSales(), report.getTotalQuantity(),
                report.getTopProducts(), report.getLowProducts(),
                report.getPeakHours(), report.getSlowHours(), report.getAiSummary()
        );

        String recipient = (toEmail != null && !toEmail.isBlank())
                ? toEmail
                : report.getBranch().getStore().getStoreOwner().getUser().getEmail();

        emailService.sendMonthlyReportEmail(
                recipient, report.getBranch().getStore().getName(), report.getBranch().getName(),
                monthName(report.getMonth()) + " " + report.getYear(),
                report.getTotalSales(), report.getTotalQuantity(), report.getTopProducts(), pdf
        );
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void verifyOwnership(Integer userId, Branch branch) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null || !branch.getStore().getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("You do not have permission to access this resource");
    }

    private Branch findBranchOrThrow(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);
        if (branch == null) throw new ApiException("Branch not found");
        return branch;
    }

    private MonthlyReport findReportOrThrow(Integer reportId) {
        MonthlyReport report = monthlyReportRepository.findMonthlyReportById(reportId);
        if (report == null) throw new ApiException("Monthly report not found");
        return report;
    }

    private SalesStats calculateStats(List<SalesRecordItem> items) {
        double totalSales = items.stream().mapToDouble(i -> i.getTotalPrice() != null
                ? i.getTotalPrice() : i.getQuantity() * i.getUnitPrice()).sum();
        int totalQuantity = items.stream().mapToInt(SalesRecordItem::getQuantity).sum();

        Map<String, Double> salesByProduct    = new LinkedHashMap<>();
        Map<String, Integer> qtyByProduct     = new LinkedHashMap<>();
        Map<Integer, Integer> quantityByHour  = new LinkedHashMap<>();

        for (SalesRecordItem item : items) {
            double itemTotal = item.getTotalPrice() != null ? item.getTotalPrice() : item.getQuantity() * item.getUnitPrice();
            salesByProduct.merge(item.getProductName(), itemTotal, Double::sum);
            qtyByProduct.merge(item.getProductName(), item.getQuantity(), Integer::sum);
            if (item.getSaleTime() != null)
                quantityByHour.merge(item.getSaleTime().getHour(), item.getQuantity(), Integer::sum);
        }

        String topProducts = formatTopProducts(salesByProduct, true);
        String lowProducts = formatTopProducts(salesByProduct, false);
        String surplus     = qtyByProduct.entrySet().stream().sorted(Map.Entry.comparingByValue())
                .limit(3).map(e -> e.getKey() + " (" + e.getValue() + " units)").collect(Collectors.joining(", "));
        String peakHours   = quantityByHour.isEmpty() ? "N/A" : formatHour(quantityByHour, true);
        String slowHours   = quantityByHour.isEmpty() ? "N/A" : formatHour(quantityByHour, false);

        return new SalesStats(totalSales, totalQuantity, topProducts, lowProducts, surplus, peakHours, slowHours);
    }

    private String generateAiSummary(Branch branch, MonthlyReportIn dto, SalesStats stats) {
        try {
            int prevMonth = dto.getMonth() == 1 ? 12 : dto.getMonth() - 1;
            int prevYear  = dto.getMonth() == 1 ? dto.getYear() - 1 : dto.getYear();
            MonthlyReport prev = monthlyReportRepository.findMonthlyReportByBranchIdAndMonthAndYear(branch.getId(), prevMonth, prevYear);
            if (prev != null) {
                return openAiService.generateMonthlyReportComparisonSummary(
                        branch.getStore().getName(), branch.getName(),
                        dto.getMonth(), dto.getYear(), stats.totalSales(), stats.totalQuantity(),
                        stats.topProducts(), stats.lowProducts(), stats.surplusProducts(),
                        stats.peakHours(), stats.slowHours(),
                        prev.getMonth(), prev.getYear(), prev.getTotalSales(), prev.getTotalQuantity(),
                        prev.getTopProducts(), prev.getLowProducts()
                );
            }
            return openAiService.generateMonthlyReportSummary(
                    branch.getStore().getName(), branch.getName(),
                    dto.getMonth(), dto.getYear(), stats.totalSales(), stats.totalQuantity(),
                    stats.topProducts(), stats.lowProducts(), stats.surplusProducts(),
                    stats.peakHours(), stats.slowHours()
            );
        } catch (Exception e) {
            return "AI summary could not be generated at this time.";
        }
    }

    private String formatTopProducts(Map<String, Double> map, boolean highest) {
        Comparator<Map.Entry<String, Double>> c = Map.Entry.comparingByValue();
        if (highest) c = c.reversed();
        return map.entrySet().stream().sorted(c).limit(3)
                .map(e -> e.getKey() + " (" + String.format("%.2f", e.getValue()) + " SAR)")
                .collect(Collectors.joining(", "));
    }

    private String formatHour(Map<Integer, Integer> map, boolean highest) {
        Comparator<Map.Entry<Integer, Integer>> c = Map.Entry.comparingByValue();
        if (highest) c = c.reversed();
        return map.entrySet().stream().sorted(c).findFirst()
                .map(e -> String.format("%02d:00 (%d units)", e.getKey(), e.getValue())).orElse("N/A");
    }

    private String monthName(Integer month) {
        return switch (month) {
            case 1 -> "January"; case 2 -> "February"; case 3 -> "March";
            case 4 -> "April";   case 5 -> "May";       case 6 -> "June";
            case 7 -> "July";    case 8 -> "August";    case 9 -> "September";
            case 10 -> "October";case 11 -> "November"; case 12 -> "December";
            default -> "Month " + month;
        };
    }

    private MonthlyReportOut mapToOut(MonthlyReport r) {
        return new MonthlyReportOut(
                r.getId(), r.getMonth(), r.getYear(), r.getTotalSales(), r.getTotalQuantity(),
                r.getTopProducts(), r.getLowProducts(), r.getPeakHours(), r.getSlowHours(),
                r.getSurplusProducts(), r.getAiSummary(), r.getPdfUrl(), r.getGeneratedAt(),
                r.getBranch().getId(), r.getBranch().getName(),
                r.getBranch().getStore().getId(), r.getBranch().getStore().getName()
        );
    }

    private record SalesStats(double totalSales, int totalQuantity, String topProducts,
                               String lowProducts, String surplusProducts, String peakHours, String slowHours) {}
}