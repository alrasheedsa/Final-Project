package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.MonthlyReportIn;
import com.example.fproject.Service.MonthlyReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/monthly-report")
@RequiredArgsConstructor
public class MonthlyReportController {

    private final MonthlyReportService monthlyReportService;

    @PostMapping("/generate/{branchId}")
    public ResponseEntity<?> generateMonthlyReport(@PathVariable Integer branchId, @Valid @RequestBody MonthlyReportIn dto) {
        monthlyReportService.generateMonthlyReport(branchId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Monthly report generated successfully"));
    }

    @PutMapping("/regenerate/{reportId}")
    public ResponseEntity<?> regenerateMonthlyReport(@PathVariable Integer reportId) {
        monthlyReportService.regenerateMonthlyReport(reportId);
        return ResponseEntity.status(200).body(new ApiResponse("Monthly report regenerated successfully"));
    }

    @GetMapping("/get")
    public ResponseEntity<?> getAllMonthlyReports() {
        return ResponseEntity.status(200).body(monthlyReportService.getAllMonthlyReports());
    }

    @GetMapping("/get/{reportId}")
    public ResponseEntity<?> getMonthlyReportById(@PathVariable Integer reportId) {
        return ResponseEntity.status(200).body(monthlyReportService.getMonthlyReportById(reportId));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<?> getMonthlyReportsByBranchId(@PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(monthlyReportService.getMonthlyReportsByBranchId(branchId));
    }

    @GetMapping("/branch/{branchId}/date")
    public ResponseEntity<?> getMonthlyReportByBranchAndDate(@PathVariable Integer branchId, @RequestParam Integer month, @RequestParam Integer year) {
        return ResponseEntity.status(200).body(monthlyReportService.getMonthlyReportByBranchAndDate(branchId, month, year));
    }

    @DeleteMapping("/delete/{reportId}")
    public ResponseEntity<?> deleteMonthlyReport(@PathVariable Integer reportId) {
        monthlyReportService.deleteMonthlyReport(reportId);
        return ResponseEntity.status(200).body(new ApiResponse("Monthly report deleted successfully"));
    }

    @GetMapping("/download/{reportId}")
    public ResponseEntity<byte[]> downloadMonthlyReport(@PathVariable Integer reportId) {
        return ResponseEntity.status(200)
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=monthly-report-" + reportId + ".pdf"
                )
                .body(monthlyReportService.downloadMonthlyReport(reportId));
    }

    @PostMapping("/send-email/{reportId}")
    public ResponseEntity<?> sendReportByEmail(@PathVariable Integer reportId, @RequestParam(required = false) String toEmail) {
        monthlyReportService.sendReportByEmail(reportId, toEmail);
        return ResponseEntity.status(200).body(new ApiResponse("Monthly report sent successfully by email"));
    }
}