package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.MonthlyReportIn;
import com.example.fproject.Model.User;
import com.example.fproject.Service.MonthlyReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/monthly-report")
@RequiredArgsConstructor
public class MonthlyReportController {

    private final MonthlyReportService monthlyReportService;

    // STORE_OWNER
    @PostMapping("/generate/{branchId}")
    public ResponseEntity<?> generateMonthlyReport(@AuthenticationPrincipal User user,
                                                   @PathVariable Integer branchId,
                                                   @Valid @RequestBody MonthlyReportIn dto) {
        monthlyReportService.generateMonthlyReport(user.getId(), branchId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Monthly report generated successfully"));
    }

    // STORE_OWNER
    @PutMapping("/regenerate/{reportId}")
    public ResponseEntity<?> regenerateMonthlyReport(@AuthenticationPrincipal User user, @PathVariable Integer reportId) {
        monthlyReportService.regenerateMonthlyReport(user.getId(), reportId);
        return ResponseEntity.status(200).body(new ApiResponse("Monthly report regenerated successfully"));
    }

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllMonthlyReports() {
        return ResponseEntity.status(200).body(monthlyReportService.getAllMonthlyReports());
    }

    // STORE_OWNER
    @GetMapping("/get/{reportId}")
    public ResponseEntity<?> getMonthlyReportById(@AuthenticationPrincipal User user, @PathVariable Integer reportId) {
        return ResponseEntity.status(200).body(monthlyReportService.getMonthlyReportById(user.getId(), reportId));
    }

    // STORE_OWNER
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<?> getMonthlyReportsByBranchId(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(monthlyReportService.getMonthlyReportsByBranchId(user.getId(), branchId));
    }

    // STORE_OWNER
    @GetMapping("/branch/{branchId}/date")
    public ResponseEntity<?> getMonthlyReportByBranchAndDate(@AuthenticationPrincipal User user,
                                                              @PathVariable Integer branchId,
                                                              @RequestParam Integer month,
                                                              @RequestParam Integer year) {
        return ResponseEntity.status(200).body(monthlyReportService.getMonthlyReportByBranchAndDate(user.getId(), branchId, month, year));
    }

    // STORE_OWNER
    @DeleteMapping("/delete/{reportId}")
    public ResponseEntity<?> deleteMonthlyReport(@AuthenticationPrincipal User user, @PathVariable Integer reportId) {
        monthlyReportService.deleteMonthlyReport(user.getId(), reportId);
        return ResponseEntity.status(200).body(new ApiResponse("Monthly report deleted successfully"));
    }

    // STORE_OWNER
    @GetMapping("/download/{reportId}")
    public ResponseEntity<byte[]> downloadMonthlyReport(@AuthenticationPrincipal User user, @PathVariable Integer reportId) {
        return ResponseEntity.status(200)
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=monthly-report-" + reportId + ".pdf")
                .body(monthlyReportService.downloadMonthlyReport(user.getId(), reportId));
    }

    // STORE_OWNER
    @PostMapping("/send-email/{reportId}")
    public ResponseEntity<?> sendReportByEmail(@AuthenticationPrincipal User user,
                                               @PathVariable Integer reportId,
                                               @RequestParam(required = false) String toEmail) {
        monthlyReportService.sendReportByEmail(user.getId(), reportId, toEmail);
        return ResponseEntity.status(200).body(new ApiResponse("Monthly report sent successfully by email"));
    }
}