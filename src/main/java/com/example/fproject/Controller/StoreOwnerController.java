package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.StoreOwnerIn;
import com.example.fproject.DTO.OUT.AuthUserOut;
import com.example.fproject.DTO.OUT.StoreOwnerOut;
import com.example.fproject.Model.User;
import com.example.fproject.Service.StoreOwnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/store-owner")
@RequiredArgsConstructor
public class StoreOwnerController {

    private final StoreOwnerService storeOwnerService;

    @PostMapping("/register")
    public ResponseEntity<?> registerStoreOwner(@Valid @RequestBody StoreOwnerIn dto) {
        StoreOwnerOut out = storeOwnerService.registerStoreOwner(dto);
        return ResponseEntity.status(200).body(new AuthUserOut(out.getId(), out.getFullName(), out.getEmail(), "STORE_OWNER"));
    }

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllStoreOwners() {
        return ResponseEntity.status(200).body(storeOwnerService.getAllStoreOwners());
    }

    // ADMIN
    @GetMapping("/get/{storeOwnerId}")
    public ResponseEntity<?> getStoreOwnerById(@PathVariable Integer storeOwnerId) {
        return ResponseEntity.status(200).body(storeOwnerService.getStoreOwnerById(storeOwnerId));
    }

    // STORE_OWNER — بياناته هو
    @GetMapping("/my")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(storeOwnerService.getMyProfile(user.getId()));
    }

    // STORE_OWNER — يعدل على نفسه
    @PutMapping("/update")
    public ResponseEntity<?> updateStoreOwner(@AuthenticationPrincipal User user, @Valid @RequestBody StoreOwnerIn dto) {
        storeOwnerService.updateStoreOwner(user.getId(), dto);
        return ResponseEntity.status(200).body(new ApiResponse("Store owner updated successfully"));
    }

    // STORE_OWNER — يحذف نفسه
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteStoreOwner(@AuthenticationPrincipal User user) {
        storeOwnerService.deleteStoreOwner(user.getId());
        return ResponseEntity.status(200).body(new ApiResponse("Store owner deleted successfully"));
    }
}