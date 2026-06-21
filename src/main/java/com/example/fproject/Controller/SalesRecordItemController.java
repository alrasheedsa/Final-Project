package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.SalesRecordItemIn;
import com.example.fproject.Model.User;
import com.example.fproject.Service.SalesRecordItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales-record-item")
@RequiredArgsConstructor
public class SalesRecordItemController {

    private final SalesRecordItemService salesRecordItemService;

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllSalesRecordItems() {
        return ResponseEntity.status(200).body(salesRecordItemService.getAllSalesRecordItems());
    }

    // STORE_OWNER
    @GetMapping("/get/{id}")
    public ResponseEntity<?> getSalesRecordItemById(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        return ResponseEntity.status(200).body(salesRecordItemService.getSalesRecordItemById(user.getId(), id));
    }

    // STORE_OWNER
    @GetMapping("/get-by-sales-record/{salesRecordId}")
    public ResponseEntity<?> getSalesRecordItemsBySalesRecordId(@AuthenticationPrincipal User user, @PathVariable Integer salesRecordId) {
        return ResponseEntity.status(200).body(salesRecordItemService.getSalesRecordItemsBySalesRecordId(user.getId(), salesRecordId));
    }

    // STORE_OWNER
    @PostMapping("/add/sales-record/{salesRecordId}")
    public ResponseEntity<?> addSalesRecordItem(@AuthenticationPrincipal User user,
                                                @PathVariable Integer salesRecordId,
                                                @RequestBody @Valid SalesRecordItemIn salesRecordItemIn) {
        salesRecordItemService.addSalesRecordItem(user.getId(), salesRecordId, salesRecordItemIn);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record item added successfully"));
    }

    // STORE_OWNER
    @PutMapping("/update/{id}/sales-record/{salesRecordId}")
    public ResponseEntity<?> updateSalesRecordItem(@AuthenticationPrincipal User user,
                                                   @PathVariable Integer id,
                                                   @PathVariable Integer salesRecordId,
                                                   @RequestBody @Valid SalesRecordItemIn salesRecordItemIn) {
        salesRecordItemService.updateSalesRecordItem(user.getId(), id, salesRecordId, salesRecordItemIn);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record item updated successfully"));
    }

    // STORE_OWNER
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteSalesRecordItem(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        salesRecordItemService.deleteSalesRecordItem(user.getId(), id);
        return ResponseEntity.status(200).body(new ApiResponse("Sales record item deleted successfully"));
    }
}