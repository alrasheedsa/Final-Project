package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.SalesRecordIn;
import com.example.fproject.Service.SalesRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/add")
    public ResponseEntity<?> addSalesRecord(@RequestBody @Valid SalesRecordIn salesRecordIn) {
        salesRecordService.addSalesRecord(salesRecordIn);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateSalesRecord(@PathVariable Integer id,
                                               @RequestBody @Valid SalesRecordIn salesRecordIn) {
        salesRecordService.updateSalesRecord(id, salesRecordIn);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteSalesRecord(@PathVariable Integer id) {
        salesRecordService.deleteSalesRecord(id);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record deleted successfully"));
    }
}
