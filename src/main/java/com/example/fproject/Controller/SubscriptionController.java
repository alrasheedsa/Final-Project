package com.example.fproject.Controller;


import com.example.fproject.Api.ApiResponse;
import com.example.fproject.Model.User;
import com.example.fproject.Service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // PUBLIC — كل المستخدمين يشوفون الخطط
    @GetMapping("/plans")
    public ResponseEntity<?> getSubscriptionPlans() {
        return ResponseEntity.status(200).body(subscriptionService.getSubscriptionPlans());
    }

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllSubscriptions() {
        return ResponseEntity.status(200).body(subscriptionService.getAllSubscriptions());
    }

    // ADMIN
    @GetMapping("/get/{subscriptionId}")
    public ResponseEntity<?> getSubscriptionById(@PathVariable Integer subscriptionId) {
        return ResponseEntity.status(200).body(subscriptionService.getSubscriptionById(subscriptionId));
    }

    // STORE_OWNER — يجيب ID من الـ token
    @GetMapping("/my")
    public ResponseEntity<?> getMySubscriptions(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(subscriptionService.getSubscriptionsByStoreOwner(user.getId()));
    }

    // STORE_OWNER — يجيب ID من الـ token
    @GetMapping("/my/active")
    public ResponseEntity<?> getMyActiveSubscription(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(subscriptionService.getActiveSubscription(user.getId()));
    }

    // STORE_OWNER — يجيب ID من الـ token
    @GetMapping("/my/status")
    public ResponseEntity<?> getMySubscriptionStatus(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(subscriptionService.getSubscriptionStatus(user.getId()));
    }

    // STORE_OWNER — يجيب ID من الـ token
    @GetMapping("/my/dashboard")
    public ResponseEntity<?> getMyDashboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(subscriptionService.getStoreOwnerDashboard(user.getId()));
    }

    // STORE_OWNER — يجيب ID من الـ token
    @GetMapping("/my/limits")
    public ResponseEntity<?> getMySubscriptionLimits(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(subscriptionService.getSubscriptionLimits(user.getId()));
    }

    // STORE_OWNER — يجيب ID من الـ token
    @GetMapping("/my/can-create-branch/{storeId}")
    public ResponseEntity<?> canCreateBranch(@AuthenticationPrincipal User user, @PathVariable Integer storeId) {
        return ResponseEntity.status(200).body(subscriptionService.canCreateBranch(user.getId(), storeId));
    }

    // STORE_OWNER — يجيب ID من الـ token
    @PostMapping("/my/renew/{newPlanType}")
    public ResponseEntity<?> renewSubscription(@AuthenticationPrincipal User user, @PathVariable String newPlanType) {
        String checkoutUrl = subscriptionService.renewSubscription(user.getId(), newPlanType);
        return ResponseEntity.status(200).body(new ApiResponse(checkoutUrl));
    }

    // STORE_OWNER
    @PutMapping("/cancel/{subscriptionId}")
    public ResponseEntity<?> cancelSubscription(@AuthenticationPrincipal User user, @PathVariable Integer subscriptionId) {
        subscriptionService.cancelSubscription(user.getId(), subscriptionId);
        return ResponseEntity.status(200).body(new ApiResponse("Subscription cancelled successfully"));
    }

    // ADMIN
    @PutMapping("/check-expired")
    public ResponseEntity<?> checkExpiredSubscriptions() {
        return ResponseEntity.status(200).body(subscriptionService.checkExpiredSubscriptions());
    }
}