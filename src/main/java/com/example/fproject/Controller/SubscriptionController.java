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

    @PostMapping("/add/{storeOwnerId}")
    public ResponseEntity<?> createSubscription(@PathVariable Integer storeOwnerId, @Valid @RequestBody SubscriptionIn dto) {
        return ResponseEntity.status(200).body(subscriptionService.createSubscription(storeOwnerId, dto));
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
    public ResponseEntity<?> getSubscriptionsByStoreOwnerId(@PathVariable Integer storeOwnerId) {
        return ResponseEntity.status(200).body(subscriptionService.getSubscriptionsByStoreOwnerId(storeOwnerId));
    }

    @PutMapping("/update/{subscriptionId}")
    public ResponseEntity<?> updateSubscription(@PathVariable Integer subscriptionId, @Valid @RequestBody SubscriptionIn dto) {
        return ResponseEntity.status(200).body(subscriptionService.updateSubscription(subscriptionId, dto));
    }

    @PutMapping("/cancel/{subscriptionId}")
    public ResponseEntity<?> cancelSubscription(@PathVariable Integer subscriptionId) {
        return ResponseEntity.status(200).body(subscriptionService.cancelSubscription(subscriptionId));
    }

    @DeleteMapping("/delete/{subscriptionId}")
    public ResponseEntity<?> deleteSubscription(@PathVariable Integer subscriptionId) {
        subscriptionService.deleteSubscription(subscriptionId);
        return ResponseEntity.status(200).body(new ApiResponse("Subscription deleted successfully"));
    }

    @GetMapping("/active/{storeOwnerId}")
    public ResponseEntity<?> getActiveSubscriptionByStoreOwnerId(@PathVariable Integer storeOwnerId) {
        return ResponseEntity.status(200).body(subscriptionService.getActiveSubscriptionByStoreOwnerId(storeOwnerId));
    }
}