package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.SubscriptionIn;
import com.example.fproject.Service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/plans")
    public ResponseEntity<?> getSubscriptionPlans() {
        return ResponseEntity.status(200).body(subscriptionService.getSubscriptionPlans());
    }

    @GetMapping("/get")
    public ResponseEntity<?> getAllSubscriptions() {
        return ResponseEntity.status(200).body(subscriptionService.getAllSubscriptions());
    }

    @GetMapping("/get/{subscriptionId}")
    public ResponseEntity<?> getSubscriptionById(@PathVariable Integer subscriptionId) {
        return ResponseEntity.status(200).body(subscriptionService.getSubscriptionById(subscriptionId));
    }

    @GetMapping("/store-owner/{storeOwnerId}")
    public ResponseEntity<?> getSubscriptionsByStoreOwner(@PathVariable Integer storeOwnerId) {
        return ResponseEntity.status(200).body(subscriptionService.getSubscriptionsByStoreOwner(storeOwnerId));
    }

    @GetMapping("/active/{storeOwnerId}")
    public ResponseEntity<?> getActiveSubscription(@PathVariable Integer storeOwnerId) {
        return ResponseEntity.status(200).body(subscriptionService.getActiveSubscription(storeOwnerId));
    }


    @GetMapping("/status/{storeOwnerId}")
    public ResponseEntity<?> getSubscriptionStatus(@PathVariable Integer storeOwnerId) {
        return ResponseEntity.status(200).body(subscriptionService.getSubscriptionStatus(storeOwnerId));
    }


    @PostMapping("/renew/{storeOwnerId}/{newPlanType}")
    public ResponseEntity<?> renewSubscription(@PathVariable Integer storeOwnerId, @PathVariable String newPlanType) {
        String checkoutUrl = subscriptionService.renewSubscription(storeOwnerId, newPlanType);
        return ResponseEntity.status(200).body(new ApiResponse(checkoutUrl));
    }


    @PutMapping("/cancel/{subscriptionId}")
    public ResponseEntity<?> cancelSubscription(@PathVariable Integer subscriptionId) {
        subscriptionService.cancelSubscription(subscriptionId);
        return ResponseEntity.status(200).body(new ApiResponse("Subscription cancelled successfully"));
    }

    @PutMapping("/expire/{subscriptionId}")
    public ResponseEntity<?> expireSubscription(@PathVariable Integer subscriptionId) {
        subscriptionService.expireSubscription(subscriptionId);
        return ResponseEntity.status(200).body(new ApiResponse("Subscription expired successfully"));
    }
}