package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.BranchIn;
import com.example.fproject.Service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/branch")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PostMapping("/add/{storeId}")
    public ResponseEntity<?> addBranch(@PathVariable Integer storeId, @Valid @RequestBody BranchIn dto) {
        branchService.addBranch(storeId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Store added successfully"));
    }

    @GetMapping("/get")
    public ResponseEntity<?> getAllBranches() {
        return ResponseEntity.status(200).body(branchService.getAllBranches());
    }

    @GetMapping("/get/{branchId}")
    public ResponseEntity<?> getBranchById(@PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(branchService.getBranchById(branchId));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<?> getBranchesByStoreId(@PathVariable Integer storeId) {
        return ResponseEntity.status(200).body(branchService.getBranchesByStoreId(storeId));
    }

    @PutMapping("/update/{branchId}")
    public ResponseEntity<?> updateBranch(@PathVariable Integer branchId, @Valid @RequestBody BranchIn dto) {
        branchService.updateBranch(branchId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Store updated successfully"));
    }

    @PutMapping("/activate/{branchId}")
    public ResponseEntity<?> activateBranch(@PathVariable Integer branchId) {
        branchService.activateBranch(branchId);
        return ResponseEntity.status(200).body(new ApiResponse("Branch activated successfully"));
    }

    @PutMapping("/deactivate/{branchId}")
    public ResponseEntity<?> deactivateBranch(@PathVariable Integer branchId) {
        branchService.deactivateBranch(branchId);
        return ResponseEntity.status(200).body(new ApiResponse("Branch deactivated successfully"));
    }

    @DeleteMapping("/delete/{branchId}")
    public ResponseEntity<?> deleteBranch(@PathVariable Integer branchId) {
        branchService.deleteBranch(branchId);
        return ResponseEntity.status(200).body(new ApiResponse("Branch deleted successfully"));
    }

    @GetMapping("/subscribed/{branchId}")
    public ResponseEntity<?> isBranchSubscribed(@PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(new ApiResponse("subscribed: "+ branchService.isBranchSubscribed(branchId)));
    }


    @GetMapping("/recommended-radius/{branchId}")
    public ResponseEntity<?> getRecommendedRadius(@PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(branchService.getRecommendedRadius(branchId));
    }

    @PutMapping("/apply-recommended-radius/{branchId}")
    public ResponseEntity<?> applyRecommendedRadius(@PathVariable Integer branchId) {
        branchService.applyRecommendedRadius(branchId);
        return ResponseEntity.status(200).body(new ApiResponse("Recommended radius Applied successfully"));
    }


    @GetMapping("/{branchId}/dashboard")
    public ResponseEntity<?> getBranchDashboard(@PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(branchService.getBranchDashboard(branchId));
    }

    @GetMapping("/{branchId}/customers-in-radius/count")
    public ResponseEntity<?> getCustomersInRadiusCount(@PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(
                new ApiResponse(String.valueOf(branchService.getCustomersInRadiusCount(branchId)))
        );
    }

    @GetMapping("/{branchId}/campaign-radius-info")
    public ResponseEntity<?> getCampaignRadiusInfo(@PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(branchService.getCampaignRadiusInfo(branchId));
    }
}