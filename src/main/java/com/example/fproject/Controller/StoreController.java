package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.StoreIn;
import com.example.fproject.Service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @PostMapping("/add/{storeOwnerId}")
    public ResponseEntity<?> addStore(@PathVariable Integer storeOwnerId, @Valid @RequestBody StoreIn dto) {
        storeService.addStore(storeOwnerId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Store added successfully"));
    }

    @GetMapping("/get")
    public ResponseEntity<?> getAllStores() {
        return ResponseEntity.status(200).body(storeService.getAllStores());
    }

    @GetMapping("/get/{storeId}")
    public ResponseEntity<?> getStoreById(@PathVariable Integer storeId) {
        return ResponseEntity.status(200).body(storeService.getStoreById(storeId));
    }

    @GetMapping("/store-owner/{storeOwnerId}")
    public ResponseEntity<?> getStoresByStoreOwnerId(@PathVariable Integer storeOwnerId) {
        return ResponseEntity.status(200).body(storeService.getStoresByStoreOwnerId(storeOwnerId));
    }

    @PutMapping("/update/{storeId}")
    public ResponseEntity<?> updateStore(@PathVariable Integer storeId, @Valid @RequestBody StoreIn dto) {
        storeService.updateStore(storeId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Store updated successfully"));
    }

    @PutMapping("/activate/{storeId}")
    public ResponseEntity<?> activateStore(@PathVariable Integer storeId) {
        storeService.activateStore(storeId);
        return ResponseEntity.status(200).body(new ApiResponse("Store activated successfully"));
    }

    @PutMapping("/deactivate/{storeId}")
    public ResponseEntity<?> deactivateStore(@PathVariable Integer storeId) {
        storeService.deactivateStore(storeId);
        return ResponseEntity.status(200).body(new ApiResponse("Store deactivated successfully"));
    }

    @DeleteMapping("/delete/{storeId}")
    public ResponseEntity<?> deleteStore(@PathVariable Integer storeId) {
        storeService.deleteStore(storeId);
        return ResponseEntity.status(200).body(new ApiResponse("Store deleted successfully"));
    }
}