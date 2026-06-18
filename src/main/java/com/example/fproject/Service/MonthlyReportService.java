package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.MonthlyReportIn;
import com.example.fproject.DTO.OUT.MonthlyReportOut;
import com.example.fproject.Model.Branch;
import com.example.fproject.Model.MonthlyReport;
import com.example.fproject.Repository.BranchRepository;
import com.example.fproject.Repository.MonthlyReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthlyReportService {

    private final MonthlyReportRepository monthlyReportRepository;
    private final BranchRepository branchRepository;

    public MonthlyReportOut generateMonthlyReport(Integer branchId, MonthlyReportIn dto) {
        Branch branch = branchRepository.findBranchById(branchId);

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        if (monthlyReportRepository.existsMonthlyReportByBranchIdAndMonthAndYear(branchId, dto.getMonth(), dto.getYear())) {
            throw new ApiException("Monthly report already exists for this month and year");
        }

        MonthlyReport monthlyReport = new MonthlyReport();
        monthlyReport.setMonth(dto.getMonth());
        monthlyReport.setYear(dto.getYear());

        monthlyReport.setTotalSales(0.0);
        monthlyReport.setTotalQuantity(0);
        monthlyReport.setTopProducts("Not generated yet");
        monthlyReport.setLowProducts("Not generated yet");
        monthlyReport.setPeakHours("Not generated yet");
        monthlyReport.setSlowHours("Not generated yet");
        monthlyReport.setSurplusProducts("Not generated yet");
        monthlyReport.setAiSummary("Monthly report generated. AI summary will be updated after sales analysis.");
        monthlyReport.setPdfUrl("Not generated yet");
        monthlyReport.setGeneratedAt(LocalDateTime.now());
        monthlyReport.setBranch(branch);

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

    public String downloadMonthlyReport(Integer reportId) {
        MonthlyReport report = monthlyReportRepository.findMonthlyReportById(reportId);

        if (report == null) {
            throw new ApiException("Monthly report not found");
        }

        if (report.getPdfUrl() == null
                || report.getPdfUrl().isBlank()
                || report.getPdfUrl().equalsIgnoreCase("Not generated yet")) {
            throw new ApiException("Monthly report PDF is not generated yet");
        }

        return report.getPdfUrl();
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