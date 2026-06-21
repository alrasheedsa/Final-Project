package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.BranchIn;
import com.example.fproject.Model.User;
import com.example.fproject.Service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/branch")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    // STORE_OWNER — يضيف فرع لمتجره (السيرفس يتحقق إن الستور يخصه)
    @PostMapping("/add/{storeId}")
    public ResponseEntity<?> addBranch(@AuthenticationPrincipal User user, @PathVariable Integer storeId, @Valid @RequestBody BranchIn dto) {
        branchService.addBranch(user.getId(), storeId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Branch added successfully"));
    }

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllBranches() {
        return ResponseEntity.status(200).body(branchService.getAllBranches());
    }

    // STORE_OWNER — يشوف فرع معين (السيرفس يتحقق إنه يخصه)
    @GetMapping("/get/{branchId}")
    public ResponseEntity<?> getBranchById(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(branchService.getBranchById(user.getId(), branchId));
    }

    // STORE_OWNER — يشوف فروع متجره
    @GetMapping("/store/{storeId}")
    public ResponseEntity<?> getBranchesByStoreId(@AuthenticationPrincipal User user, @PathVariable Integer storeId) {
        return ResponseEntity.status(200).body(branchService.getBranchesByStoreId(user.getId(), storeId));
    }

    // STORE_OWNER
    @PutMapping("/update/{branchId}")
    public ResponseEntity<?> updateBranch(@AuthenticationPrincipal User user, @PathVariable Integer branchId, @Valid @RequestBody BranchIn dto) {
        branchService.updateBranch(user.getId(), branchId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Branch updated successfully"));
    }

    // STORE_OWNER
    @PutMapping("/activate/{branchId}")
    public ResponseEntity<?> activateBranch(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        branchService.activateBranch(user.getId(), branchId);
        return ResponseEntity.status(200).body(new ApiResponse("Branch activated successfully"));
    }

    // STORE_OWNER
    @PutMapping("/deactivate/{branchId}")
    public ResponseEntity<?> deactivateBranch(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        branchService.deactivateBranch(user.getId(), branchId);
        return ResponseEntity.status(200).body(new ApiResponse("Branch deactivated successfully"));
    }

    // STORE_OWNER
    @DeleteMapping("/delete/{branchId}")
    public ResponseEntity<?> deleteBranch(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        branchService.deleteBranch(user.getId(), branchId);
        return ResponseEntity.status(200).body(new ApiResponse("Branch deleted successfully"));
    }

    // STORE_OWNER
    @GetMapping("/subscribed/{branchId}")
    public ResponseEntity<?> isBranchSubscribed(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(new ApiResponse("subscribed: " + branchService.isBranchSubscribed(user.getId(), branchId)));
    }

    // STORE_OWNER
    @GetMapping("/recommended-radius/{branchId}")
    public ResponseEntity<?> getRecommendedRadius(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(branchService.getRecommendedRadius(user.getId(), branchId));
    }

    // STORE_OWNER
    @PutMapping("/apply-recommended-radius/{branchId}")
    public ResponseEntity<?> applyRecommendedRadius(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        branchService.applyRecommendedRadius(user.getId(), branchId);
        return ResponseEntity.status(200).body(new ApiResponse("Recommended radius applied successfully"));
    }

    // STORE_OWNER
    @GetMapping("/{branchId}/dashboard")
    public ResponseEntity<?> getBranchDashboard(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(branchService.getBranchDashboard(user.getId(), branchId));
    }

    // STORE_OWNER
    @GetMapping("/{branchId}/customers-in-radius/count")
    public ResponseEntity<?> getCustomersInRadiusCount(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(new ApiResponse(String.valueOf(branchService.getCustomersInRadiusCount(user.getId(), branchId))));
    }

    // STORE_OWNER
    @GetMapping("/{branchId}/campaign-radius-info")
    public ResponseEntity<?> getCampaignRadiusInfo(@AuthenticationPrincipal User user, @PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(branchService.getCampaignRadiusInfo(user.getId(), branchId));
    }
}