package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.StoreOwnerIn;
import com.example.fproject.Service.StoreOwnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/store-owner")
@RequiredArgsConstructor
public class StoreOwnerController {

    private final StoreOwnerService storeOwnerService;

    @PostMapping("/register")
    public ResponseEntity<?> registerStoreOwner(@Valid @RequestBody StoreOwnerIn dto) {
        return ResponseEntity.status(200).body(storeOwnerService.registerStoreOwner(dto));
    }

    @GetMapping("/get")
    public ResponseEntity<?> getAllStoreOwners() {
        return ResponseEntity.status(200).body(storeOwnerService.getAllStoreOwners());
    }

    @GetMapping("/get/{storeOwnerId}")
    public ResponseEntity<?> getStoreOwnerById(@PathVariable Integer storeOwnerId) {
        return ResponseEntity.status(200).body(storeOwnerService.getStoreOwnerById(storeOwnerId));
    }

    @PutMapping("/update/{storeOwnerId}")
    public ResponseEntity<?> updateStoreOwner(@PathVariable Integer storeOwnerId, @Valid @RequestBody StoreOwnerIn dto) {
        return ResponseEntity.status(200).body(storeOwnerService.updateStoreOwner(storeOwnerId, dto));
    }

    @DeleteMapping("/delete/{storeOwnerId}")
    public ResponseEntity<?> deleteStoreOwner(@PathVariable Integer storeOwnerId) {
        storeOwnerService.deleteStoreOwner(storeOwnerId);
        return ResponseEntity.status(200).body(new ApiResponse("Store owner deleted successfully"));
    }
}