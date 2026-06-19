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


    public MonthlyReportOut generateMonthlyReport(Integer branchId, MonthlyReportIn dto) {
        Branch branch = branchRepository.findBranchById(branchId);

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        if (!branchService.isBranchSubscribed(branchId)) {
            throw new ApiException("Branch must have an active subscription to generate a monthly report");
        }

        if (monthlyReportRepository.existsMonthlyReportByBranchIdAndMonthAndYear(branchId, dto.getMonth(), dto.getYear())) {
            throw new ApiException("Monthly report already exists for this month and year");
        }

        SalesRecord salesRecord =
                salesRecordRepository.findByBranch_IdAndMonthAndYear(branchId, dto.getMonth(), dto.getYear());

        if (salesRecord == null) {
            throw new ApiException("No sales record found for this branch, month, and year");
        }

        List<SalesRecordItem> items =
                salesRecordItemRepository.findAllBySalesRecord_Id(salesRecord.getId())
                        .stream()
                        .filter(item -> item.getSaleDate() != null
                                && item.getSaleDate().getMonthValue() == dto.getMonth()
                                && item.getSaleDate().getYear() == dto.getYear())
                        .toList();

        if (items.isEmpty()) {
            throw new ApiException("No sales data found for the selected month");
        }

        double totalSales = items.stream()
                .mapToDouble(item -> item.getTotalPrice() != null
                        ? item.getTotalPrice()
                        : item.getQuantity() * item.getUnitPrice())
                .sum();

        int totalQuantity = items.stream()
                .mapToInt(SalesRecordItem::getQuantity)
                .sum();

        Map<String, Double> salesByProduct = new LinkedHashMap<>();
        Map<String, Integer> quantityByProduct = new LinkedHashMap<>();
        Map<Integer, Integer> quantityByHour = new LinkedHashMap<>();

        for (SalesRecordItem item : items) {
            double itemTotal = item.getTotalPrice() != null
                    ? item.getTotalPrice()
                    : item.getQuantity() * item.getUnitPrice();

            salesByProduct.merge(item.getProductName(), itemTotal, Double::sum);
            quantityByProduct.merge(item.getProductName(), item.getQuantity(), Integer::sum);
            quantityByHour.merge(item.getSaleTime().getHour(), item.getQuantity(), Integer::sum);
        }

        String topProducts = formatProducts(salesByProduct, true);
        String lowProducts = formatProducts(salesByProduct, false);
        String surplusProducts = quantityByProduct.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(3)
                .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
                .collect(Collectors.joining(", "));
        String peakHours = formatHour(quantityByHour, true);
        String slowHours = formatHour(quantityByHour, false);

        MonthlyReport monthlyReport = new MonthlyReport();
        monthlyReport.setMonth(dto.getMonth());
        monthlyReport.setYear(dto.getYear());

        monthlyReport.setTotalSales(totalSales);
        monthlyReport.setTotalQuantity(totalQuantity);
        monthlyReport.setTopProducts(topProducts);
        monthlyReport.setLowProducts(lowProducts);
        monthlyReport.setPeakHours(peakHours);
        monthlyReport.setSlowHours(slowHours);
        monthlyReport.setSurplusProducts(surplusProducts);
        monthlyReport.setAiSummary(
                openAiService.generateMonthlyReportSummary(
                        branch.getStore().getName(),
                        branch.getName(),
                        dto.getMonth(),
                        dto.getYear(),
                        totalSales,
                        totalQuantity,
                        topProducts,
                        lowProducts,
                        surplusProducts,
                        peakHours,
                        slowHours
                )
        );
        monthlyReport.setPdfUrl("pending");
        monthlyReport.setGeneratedAt(LocalDateTime.now());
        monthlyReport.setBranch(branch);

        monthlyReportRepository.save(monthlyReport);
        monthlyReport.setPdfUrl("/api/v1/monthly-report/download/" + monthlyReport.getId());
        monthlyReportRepository.save(monthlyReport);

        return mapToDTOOUT(monthlyReport);
    }

    public List<MonthlyReportOut> getAllMonthlyReports() {
        List<MonthlyReport> reports = monthlyReportRepository.findAll();
        List<MonthlyReportOut> result = new ArrayList<>();

        for (MonthlyReport report : reports) {
            result.add(mapToDTOOUT(report));
        }

        return result;
    }

    public MonthlyReportOut getMonthlyReportById(Integer reportId) {
        MonthlyReport report = monthlyReportRepository.findMonthlyReportById(reportId);

        if (report == null) {
            throw new ApiException("Monthly report not found");
        }

        return mapToDTOOUT(report);
    }

    public List<MonthlyReportOut> getMonthlyReportsByBranchId(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        List<MonthlyReport> reports =
                monthlyReportRepository.findMonthlyReportsByBranchIdOrderByYearDescMonthDesc(branchId);

        List<MonthlyReportOut> result = new ArrayList<>();

        for (MonthlyReport report : reports) {
            result.add(mapToDTOOUT(report));
        }

        return result;
    }

    public MonthlyReportOut getMonthlyReportByBranchAndDate(Integer branchId, Integer month, Integer year) {
        Branch branch = branchRepository.findBranchById(branchId);

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        MonthlyReport report =
                monthlyReportRepository.findMonthlyReportByBranchIdAndMonthAndYear(branchId, month, year);

        if (report == null) {
            throw new ApiException("Monthly report not found");
        }

        return mapToDTOOUT(report);
    }

    public MonthlyReportOut updateMonthlyReport(Integer reportId, MonthlyReportIn dto) {
        MonthlyReport report = monthlyReportRepository.findMonthlyReportById(reportId);

        if (report == null) {
            throw new ApiException("Monthly report not found");
        }

        if (!report.getMonth().equals(dto.getMonth()) || !report.getYear().equals(dto.getYear())) {
            boolean exists = monthlyReportRepository.existsMonthlyReportByBranchIdAndMonthAndYear(
                    report.getBranch().getId(),
                    dto.getMonth(),
                    dto.getYear()
            );

            if (exists) {
                throw new ApiException("Monthly report already exists for this month and year");
            }
        }

        report.setMonth(dto.getMonth());
        report.setYear(dto.getYear());
        report.setGeneratedAt(LocalDateTime.now());

        monthlyReportRepository.save(report);

        return mapToDTOOUT(report);
    }

    public void deleteMonthlyReport(Integer reportId) {
        MonthlyReport report = monthlyReportRepository.findMonthlyReportById(reportId);

        if (report == null) {
            throw new ApiException("Monthly report not found");
        }

        monthlyReportRepository.delete(report);
    }

    public byte[] downloadMonthlyReport(Integer reportId) {
        MonthlyReport report = monthlyReportRepository.findMonthlyReportById(reportId);

        if (report == null) {
            throw new ApiException("Monthly report not found");
        }

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

    private String formatProducts(Map<String, Double> salesByProduct, boolean highestFirst) {
        Comparator<Map.Entry<String, Double>> comparator = Map.Entry.comparingByValue();
        if (highestFirst) {
            comparator = comparator.reversed();
        }

        return salesByProduct.entrySet().stream()
                .sorted(comparator)
                .limit(3)
                .map(entry -> entry.getKey() + " (" + String.format("%.2f", entry.getValue()) + " SAR)")
                .collect(Collectors.joining(", "));
    }

    private String formatHour(Map<Integer, Integer> quantityByHour, boolean highestFirst) {
        Comparator<Map.Entry<Integer, Integer>> comparator = Map.Entry.comparingByValue();
        if (highestFirst) {
            comparator = comparator.reversed();
        }

        Map.Entry<Integer, Integer> result = quantityByHour.entrySet().stream()
                .sorted(comparator)
                .findFirst()
                .orElseThrow(() -> new ApiException("Sales hours are missing"));

        return String.format("%02d:00 (%d units)", result.getKey(), result.getValue());
    }

    private MonthlyReportOut mapToDTOOUT(MonthlyReport report) {
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
}
