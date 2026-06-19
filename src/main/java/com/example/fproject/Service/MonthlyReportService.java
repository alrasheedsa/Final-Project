package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.MonthlyReportIn;
import com.example.fproject.DTO.OUT.MonthlyReportOut;
import com.example.fproject.Model.Branch;
import com.example.fproject.Model.MonthlyReport;
import com.example.fproject.Model.SalesRecord;
import com.example.fproject.Model.SalesRecordItem;
import com.example.fproject.Repository.BranchRepository;
import com.example.fproject.Repository.MonthlyReportRepository;
import com.example.fproject.Repository.SalesRecordItemRepository;
import com.example.fproject.Repository.SalesRecordRepository;
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
    private final BranchService branchService;
    private final ITextService iTextService;
    private final OpenAiService openAiService;

    @Transactional
    public MonthlyReportOut generateMonthlyReport(Integer branchId, MonthlyReportIn dto) {

        Branch branch = findBranchOrThrow(branchId);

        if (!branchService.isBranchSubscribed(branchId)) {
            throw new ApiException("Branch must have an active subscription to generate a monthly report");
        }

        if (monthlyReportRepository.existsMonthlyReportByBranchIdAndMonthAndYear(
                branchId, dto.getMonth(), dto.getYear())) {
            throw new ApiException(
                    "A monthly report already exists for " + monthName(dto.getMonth()) + " " + dto.getYear()
            );
        }

        SalesRecord salesRecord = salesRecordRepository
                .findByBranch_IdAndMonthAndYear(branchId, dto.getMonth(), dto.getYear());

        if (salesRecord == null) {
            throw new ApiException(
                    "No sales record found for " + monthName(dto.getMonth()) + " " + dto.getYear()
            );
        }

        List<SalesRecordItem> items = salesRecordItemRepository
                .findAllBySalesRecord_Id(salesRecord.getId())
                .stream()
                .filter(item -> item.getSaleDate() != null
                        && item.getSaleDate().getMonthValue() == dto.getMonth()
                        && item.getSaleDate().getYear() == dto.getYear())
                .toList();

        if (items.isEmpty()) {
            throw new ApiException(
                    "No sales items found for " + monthName(dto.getMonth()) + " " + dto.getYear()
            );
        }

        SalesStats stats = calculateStats(items);

        String aiSummary = generateAiSummary(branch, dto, stats);

        MonthlyReport report = new MonthlyReport();
        report.setMonth(dto.getMonth());
        report.setYear(dto.getYear());
        report.setTotalSales(stats.totalSales());
        report.setTotalQuantity(stats.totalQuantity());
        report.setTopProducts(stats.topProducts());
        report.setLowProducts(stats.lowProducts());
        report.setPeakHours(stats.peakHours());
        report.setSlowHours(stats.slowHours());
        report.setSurplusProducts(stats.surplusProducts());
        report.setAiSummary(aiSummary);
        report.setGeneratedAt(LocalDateTime.now());
        report.setBranch(branch);

        monthlyReportRepository.save(report);
        report.setPdfUrl("/api/v1/monthly-report/download/" + report.getId());
        monthlyReportRepository.save(report);

        return mapToOut(report);
    }

    public List<MonthlyReportOut> getAllMonthlyReports() {
        return monthlyReportRepository.findAll()
                .stream()
                .map(this::mapToOut)
                .toList();
    }

    public MonthlyReportOut getMonthlyReportById(Integer reportId) {
        return mapToOut(findReportOrThrow(reportId));
    }

    public List<MonthlyReportOut> getMonthlyReportsByBranchId(Integer branchId) {
        findBranchOrThrow(branchId);
        return monthlyReportRepository
                .findMonthlyReportsByBranchIdOrderByYearDescMonthDesc(branchId)
                .stream()
                .map(this::mapToOut)
                .toList();
    }

    public MonthlyReportOut getMonthlyReportByBranchAndDate(
            Integer branchId, Integer month, Integer year) {

        findBranchOrThrow(branchId);

        MonthlyReport report = monthlyReportRepository
                .findMonthlyReportByBranchIdAndMonthAndYear(branchId, month, year);

        if (report == null) {
            throw new ApiException(
                    "No report found for " + monthName(month) + " " + year
            );
        }

        return mapToOut(report);
    }

    @Transactional
    public MonthlyReportOut regenerateMonthlyReport(Integer reportId) {

        MonthlyReport existing = findReportOrThrow(reportId);

        Branch branch = existing.getBranch();
        Integer branchId = branch.getId();
        Integer month    = existing.getMonth();
        Integer year     = existing.getYear();

        if (!branchService.isBranchSubscribed(branchId)) {
            throw new ApiException("Branch must have an active subscription to regenerate a report");
        }

        SalesRecord salesRecord = salesRecordRepository
                .findByBranch_IdAndMonthAndYear(branchId, month, year);

        if (salesRecord == null) {
            throw new ApiException(
                    "No sales record found for " + monthName(month) + " " + year
            );
        }

        List<SalesRecordItem> items = salesRecordItemRepository
                .findAllBySalesRecord_Id(salesRecord.getId())
                .stream()
                .filter(item -> item.getSaleDate() != null
                        && item.getSaleDate().getMonthValue() == month
                        && item.getSaleDate().getYear() == year)
                .toList();

        if (items.isEmpty()) {
            throw new ApiException(
                    "No sales items found for " + monthName(month) + " " + year
            );
        }

        SalesStats stats = calculateStats(items);
        String aiSummary = generateAiSummary(
                branch,
                new MonthlyReportIn(month, year),
                stats
        );

        existing.setTotalSales(stats.totalSales());
        existing.setTotalQuantity(stats.totalQuantity());
        existing.setTopProducts(stats.topProducts());
        existing.setLowProducts(stats.lowProducts());
        existing.setPeakHours(stats.peakHours());
        existing.setSlowHours(stats.slowHours());
        existing.setSurplusProducts(stats.surplusProducts());
        existing.setAiSummary(aiSummary);
        existing.setGeneratedAt(LocalDateTime.now());

        monthlyReportRepository.save(existing);

        return mapToOut(existing);
    }


    @Transactional
    public void deleteMonthlyReport(Integer reportId) {
        MonthlyReport report = findReportOrThrow(reportId);
        monthlyReportRepository.delete(report);
    }


    public byte[] downloadMonthlyReport(Integer reportId) {

        MonthlyReport report = findReportOrThrow(reportId);

        return iTextService.generateSalesMonthlyReportPdf(
                report.getBranch().getStore().getName(),
                report.getBranch().getName(),
                report.getMonth(),
                report.getYear(),
                report.getTotalSales(),
                report.getTotalQuantity(),
                report.getTopProducts(),
                report.getLowProducts(),
                report.getPeakHours(),
                report.getSlowHours(),
                report.getAiSummary()
        );
    }


    private SalesStats calculateStats(List<SalesRecordItem> items) {

        double totalSales = items.stream()
                .mapToDouble(item -> item.getTotalPrice() != null
                        ? item.getTotalPrice()
                        : item.getQuantity() * item.getUnitPrice())
                .sum();

        int totalQuantity = items.stream()
                .mapToInt(SalesRecordItem::getQuantity)
                .sum();

        Map<String, Double>  salesByProduct    = new LinkedHashMap<>();
        Map<String, Integer> quantityByProduct = new LinkedHashMap<>();
        Map<Integer, Integer> quantityByHour   = new LinkedHashMap<>();

        for (SalesRecordItem item : items) {
            double itemTotal = item.getTotalPrice() != null
                    ? item.getTotalPrice()
                    : item.getQuantity() * item.getUnitPrice();

            salesByProduct.merge(item.getProductName(), itemTotal, Double::sum);
            quantityByProduct.merge(item.getProductName(), item.getQuantity(), Integer::sum);

            if (item.getSaleTime() != null) {
                quantityByHour.merge(item.getSaleTime().getHour(), item.getQuantity(), Integer::sum);
            }
        }

        String topProducts = formatTopProducts(salesByProduct, true);
        String lowProducts = formatTopProducts(salesByProduct, false);

        String surplusProducts = quantityByProduct.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(3)
                .map(e -> e.getKey() + " (" + e.getValue() + " units)")
                .collect(Collectors.joining(", "));

        String peakHours = quantityByHour.isEmpty() ? "N/A" : formatHour(quantityByHour, true);
        String slowHours = quantityByHour.isEmpty() ? "N/A" : formatHour(quantityByHour, false);

        return new SalesStats(
                totalSales, totalQuantity,
                topProducts, lowProducts, surplusProducts,
                peakHours, slowHours
        );
    }


    private String generateAiSummary(Branch branch, MonthlyReportIn dto, SalesStats stats) {
        try {
            return openAiService.generateMonthlyReportSummary(
                    branch.getStore().getName(),
                    branch.getName(),
                    dto.getMonth(),
                    dto.getYear(),
                    stats.totalSales(),
                    stats.totalQuantity(),
                    stats.topProducts(),
                    stats.lowProducts(),
                    stats.surplusProducts(),
                    stats.peakHours(),
                    stats.slowHours()
            );
        } catch (Exception e) {
            return "AI summary could not be generated at this time.";
        }
    }

    private String formatTopProducts(Map<String, Double> salesByProduct, boolean highestFirst) {

        Comparator<Map.Entry<String, Double>> comparator = Map.Entry.comparingByValue();
        if (highestFirst) comparator = comparator.reversed();

        return salesByProduct.entrySet().stream()
                .sorted(comparator)
                .limit(3)
                .map(e -> e.getKey() + " (" + String.format("%.2f", e.getValue()) + " SAR)")
                .collect(Collectors.joining(", "));
    }

    private String formatHour(Map<Integer, Integer> quantityByHour, boolean highestFirst) {

        Comparator<Map.Entry<Integer, Integer>> comparator = Map.Entry.comparingByValue();
        if (highestFirst) comparator = comparator.reversed();

        return quantityByHour.entrySet().stream()
                .sorted(comparator)
                .findFirst()
                .map(e -> String.format("%02d:00 (%d units)", e.getKey(), e.getValue()))
                .orElse("N/A");
    }

    private String monthName(Integer month) {
        return switch (month) {
            case 1  -> "January";
            case 2  -> "February";
            case 3  -> "March";
            case 4  -> "April";
            case 5  -> "May";
            case 6  -> "June";
            case 7  -> "July";
            case 8  -> "August";
            case 9  -> "September";
            case 10 -> "October";
            case 11 -> "November";
            case 12 -> "December";
            default -> "Month " + month;
        };
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

    private MonthlyReportOut mapToOut(MonthlyReport report) {
        return new MonthlyReportOut(
                report.getId(),
                report.getMonth(),
                report.getYear(),
                report.getTotalSales(),
                report.getTotalQuantity(),
                report.getTopProducts(),
                report.getLowProducts(),
                report.getPeakHours(),
                report.getSlowHours(),
                report.getSurplusProducts(),
                report.getAiSummary(),
                report.getPdfUrl(),
                report.getGeneratedAt(),
                report.getBranch().getId(),
                report.getBranch().getName(),
                report.getBranch().getStore().getId(),
                report.getBranch().getStore().getName()
        );
    }

    private record SalesStats(
            double  totalSales,
            int     totalQuantity,
            String  topProducts,
            String  lowProducts,
            String  surplusProducts,
            String  peakHours,
            String  slowHours
    ) {}
}