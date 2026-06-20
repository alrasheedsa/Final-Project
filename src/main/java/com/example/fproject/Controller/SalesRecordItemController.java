package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.SalesRecordItemIn;
import com.example.fproject.Service.SalesRecordItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales-record-item")
@RequiredArgsConstructor
public class SalesRecordItemController {

    private final SalesRecordItemService salesRecordItemService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllSalesRecordItems() {
        return ResponseEntity.status(200).body(salesRecordItemService.getAllSalesRecordItems());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getSalesRecordItemById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(salesRecordItemService.getSalesRecordItemById(id));
    }

    @GetMapping("/get-by-sales-record/{salesRecordId}")
    public ResponseEntity<?> getSalesRecordItemsBySalesRecordId(@PathVariable Integer salesRecordId) {
        return ResponseEntity.status(200).body(salesRecordItemService.getSalesRecordItemsBySalesRecordId(salesRecordId));
    }

    @PostMapping("/add/sales-record/{salesRecordId}")
    public ResponseEntity<?> addSalesRecordItem(@PathVariable Integer salesRecordId,
                                                @RequestBody @Valid SalesRecordItemIn salesRecordItemIn) {
        salesRecordItemService.addSalesRecordItem(salesRecordId, salesRecordItemIn);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record item added successfully"));
    }

    @PutMapping("/update/{id}/sales-record/{salesRecordId}")
    public ResponseEntity<?> updateSalesRecordItem(@PathVariable Integer id,
                                                   @PathVariable Integer salesRecordId,
                                                   @RequestBody @Valid SalesRecordItemIn salesRecordItemIn) {
        salesRecordItemService.updateSalesRecordItem(id, salesRecordId, salesRecordItemIn);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record item updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteSalesRecordItem(@PathVariable Integer id) {
        salesRecordItemService.deleteSalesRecordItem(id);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record item deleted successfully"));
    }
}
