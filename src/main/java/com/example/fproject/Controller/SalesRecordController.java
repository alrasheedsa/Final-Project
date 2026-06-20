package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.SalesRecordIn;
import com.example.fproject.Service.SalesRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.fproject.DTO.IN.GoogleSheetSalesRecordIn;

@RestController
@RequestMapping("/api/v1/sales-record")
@RequiredArgsConstructor
public class SalesRecordController {

    private final SalesRecordService salesRecordService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllSalesRecords() {
        return ResponseEntity.status(200).body(salesRecordService.getAllSalesRecords());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getSalesRecordById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(salesRecordService.getSalesRecordById(id));
    }

    @GetMapping("/get-by-branch/{branchId}")
    public ResponseEntity<?> getSalesRecordsByBranchId(@PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(salesRecordService.getSalesRecordsByBranchId(branchId));
    }

    @PostMapping(value = "/add/branch/{branchId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addSalesRecord(@PathVariable Integer branchId,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam Integer month,
                                            @RequestParam Integer year) {
        SalesRecordIn salesRecordIn = new SalesRecordIn(month, year, branchId);
        salesRecordService.addSalesRecord(file, salesRecordIn);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record added successfully"));
    }

    @PostMapping("/import-google-sheet/branch/{branchId}")
    public ResponseEntity<?> importSalesRecordFromGoogleSheet(@PathVariable Integer branchId,
                                                              @Valid @RequestBody GoogleSheetSalesRecordIn googleSheetSalesRecordIn) {
        salesRecordService.importSalesRecordFromGoogleSheet(branchId, googleSheetSalesRecordIn);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record imported from Google Sheets successfully"));
    }

    @PutMapping(value = "/update/{id}/branch/{branchId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateSalesRecord(@PathVariable Integer id,
                                               @PathVariable Integer branchId,
                                               @RequestParam(value = "file", required = false) MultipartFile file,
                                               @RequestParam Integer month,
                                               @RequestParam Integer year) {
        SalesRecordIn salesRecordIn = new SalesRecordIn(month, year, branchId);
        salesRecordService.updateSalesRecord(id, file, salesRecordIn);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteSalesRecord(@PathVariable Integer id) {
        salesRecordService.deleteSalesRecord(id);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record deleted successfully"));
    }
}
