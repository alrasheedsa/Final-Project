package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.MonthlyReportIn;
import com.example.fproject.Service.MonthlyReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/monthly-report")
@RequiredArgsConstructor
public class MonthlyReportController {

    private final MonthlyReportService monthlyReportService;

    @PostMapping("/generate/{storeId}")
    public ResponseEntity<?> generateMonthlyReport(@PathVariable Integer storeId, @Valid @RequestBody MonthlyReportIn dto) {
        return ResponseEntity.status(200).body(monthlyReportService.generateMonthlyReport(storeId, dto));
    }

    @GetMapping("/get")
    public ResponseEntity<?> getAllMonthlyReports() {
        return ResponseEntity.status(200).body(monthlyReportService.getAllMonthlyReports());
    }

    @GetMapping("/get/{reportId}")
    public ResponseEntity<?> getMonthlyReportById(@PathVariable Integer reportId) {
        return ResponseEntity.status(200).body(monthlyReportService.getMonthlyReportById(reportId));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<?> getMonthlyReportsByStoreId(@PathVariable Integer storeId) {
        return ResponseEntity.status(200).body(monthlyReportService.getMonthlyReportsByStoreId(storeId));
    }

    @GetMapping("/store/{storeId}/date")
    public ResponseEntity<?> getMonthlyReportByStoreAndDate(@PathVariable Integer storeId, @PathVariable Integer month, @PathVariable Integer year) {
        return ResponseEntity.status(200).body(
                monthlyReportService.getMonthlyReportByStoreAndDate(storeId, month, year)
        );
    }

    @PutMapping("/update/{reportId}")
    public ResponseEntity<?> updateMonthlyReport(@PathVariable Integer reportId, @Valid @RequestBody MonthlyReportIn dto) {
        return ResponseEntity.status(200).body(monthlyReportService.updateMonthlyReport(reportId, dto));
    }

    @DeleteMapping("/delete/{reportId}")
    public ResponseEntity<?> deleteMonthlyReport(@PathVariable Integer reportId) {
        monthlyReportService.deleteMonthlyReport(reportId);
        return ResponseEntity.status(200).body(new ApiResponse("Monthly report deleted successfully"));
    }
}