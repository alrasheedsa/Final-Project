package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.GoogleSheetSalesRecordIn;
import com.example.fproject.DTO.IN.SalesRecordIn;
import com.example.fproject.Model.User;
import com.example.fproject.Service.SalesRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/sales-record")
@RequiredArgsConstructor
public class SalesRecordController {

    private final SalesRecordService salesRecordService;

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllSalesRecords() {
        return ResponseEntity.status(200).body(salesRecordService.getAllSalesRecords());
    }

    // STORE_OWNER
    @GetMapping("/get/{id}")
    public ResponseEntity<?> getSalesRecordById(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        return ResponseEntity.status(200).body(salesRecordService.getSalesRecordById(user.getId(), id));
    }

    // STORE_OWNER
    @GetMapping("/get-by-branch/{branchId}")
    public ResponseEntity<?> getSalesRecordsByBranchId(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(salesRecordService.getSalesRecordsByBranchId(user.getId(), branchId));
    }

    // STORE_OWNER
    @PostMapping(value = "/add/branch/{branchId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addSalesRecord(@AuthenticationPrincipal User user,
                                            @PathVariable Integer branchId,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam Integer month,
                                            @RequestParam Integer year) {
        SalesRecordIn salesRecordIn = new SalesRecordIn(month, year, branchId);
        salesRecordService.addSalesRecord(user.getId(), file, salesRecordIn);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record added successfully"));
    }

    // STORE_OWNER
    @PostMapping("/import-google-sheet/branch/{branchId}")
    public ResponseEntity<?> importSalesRecordFromGoogleSheet(@AuthenticationPrincipal User user,
                                                              @PathVariable Integer branchId,
                                                              @Valid @RequestBody GoogleSheetSalesRecordIn googleSheetSalesRecordIn) {
        salesRecordService.importSalesRecordFromGoogleSheet(user.getId(), branchId, googleSheetSalesRecordIn);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record imported from Google Sheets successfully"));
    }

    // STORE_OWNER
    @PutMapping(value = "/update/{id}/branch/{branchId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateSalesRecord(@AuthenticationPrincipal User user,
                                               @PathVariable Integer id,
                                               @PathVariable Integer branchId,
                                               @RequestParam(value = "file", required = false) MultipartFile file,
                                               @RequestParam Integer month,
                                               @RequestParam Integer year) {
        SalesRecordIn salesRecordIn = new SalesRecordIn(month, year, branchId);
        salesRecordService.updateSalesRecord(user.getId(), id, file, salesRecordIn);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record updated successfully"));
    }

    // STORE_OWNER
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteSalesRecord(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        salesRecordService.deleteSalesRecord(user.getId(), id);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record deleted successfully"));
    }
}