package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.StoreIn;
import com.example.fproject.Model.User;
import com.example.fproject.Service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    // STORE_OWNER — يضيف متجر لنفسه
    @PostMapping("/add")
    public ResponseEntity<?> addStore(@AuthenticationPrincipal User user, @Valid @RequestBody StoreIn dto) {
        storeService.addStore(user.getId(), dto);
        return ResponseEntity.status(200).body(new ApiResponse("Store added successfully"));
    }

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllStores() {
        return ResponseEntity.status(200).body(storeService.getAllStores());
    }

    // STORE_OWNER — يشوف متجر معين (السيرفس يتحقق إنه يخصه)
    @GetMapping("/get/{storeId}")
    public ResponseEntity<?> getStoreById(@AuthenticationPrincipal User user, @PathVariable Integer storeId) {
        return ResponseEntity.status(200).body(storeService.getStoreById(user.getId(), storeId));
    }

    // STORE_OWNER — يشوف كل متاجره
    @GetMapping("/my-stores")
    public ResponseEntity<?> getMyStores(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(storeService.getStoresByStoreOwnerId(user.getId()));
    }

    // STORE_OWNER — يعدل على متجره (السيرفس يتحقق إنه يخصه)
    @PutMapping("/update/{storeId}")
    public ResponseEntity<?> updateStore(@AuthenticationPrincipal User user, @PathVariable Integer storeId, @Valid @RequestBody StoreIn dto) {
        storeService.updateStore(user.getId(), storeId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Store updated successfully"));
    }

    // ADMIN
    @PutMapping("/activate/{storeId}")
    public ResponseEntity<?> activateStore(@PathVariable Integer storeId) {
        storeService.activateStore(storeId);
        return ResponseEntity.status(200).body(new ApiResponse("Store activated successfully"));
    }

    // ADMIN
    @PutMapping("/deactivate/{storeId}")
    public ResponseEntity<?> deactivateStore(@PathVariable Integer storeId) {
        storeService.deactivateStore(storeId);
        return ResponseEntity.status(200).body(new ApiResponse("Store deactivated successfully"));
    }

    // STORE_OWNER — يحذف متجره (السيرفس يتحقق إنه يخصه)
    @DeleteMapping("/delete/{storeId}")
    public ResponseEntity<?> deleteStore(@AuthenticationPrincipal User user, @PathVariable Integer storeId) {
        storeService.deleteStore(user.getId(), storeId);
        return ResponseEntity.status(200).body(new ApiResponse("Store deleted successfully"));
    }
}