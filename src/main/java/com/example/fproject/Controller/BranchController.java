package com.example.fproject.Controller;

import com.example.fproject.DTO.IN.BranchIn;
import com.example.fproject.Service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/branch")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PostMapping("/add/{storeId}")
    public ResponseEntity<?> addBranch(@PathVariable Integer storeId, @Valid @RequestBody BranchIn dto) {
        return ResponseEntity.status(200).body(branchService.addBranch(storeId, dto));
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
        return ResponseEntity.status(200).body(branchService.updateBranch(branchId, dto));
    }

    @PutMapping("/activate/{branchId}")
    public ResponseEntity<?> activateBranch(@PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(branchService.activateBranch(branchId));
    }

    @PutMapping("/deactivate/{branchId}")
    public ResponseEntity<?> deactivateBranch(@PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(branchService.deactivateBranch(branchId));
    }

    @DeleteMapping("/delete/{branchId}")
    public ResponseEntity<?> deleteBranch(@PathVariable Integer branchId) {
        branchService.deleteBranch(branchId);
        return ResponseEntity.status(200).body("Branch deleted successfully");
    }
}