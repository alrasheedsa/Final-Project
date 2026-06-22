package com.example.fproject.Controller;

import com.example.fproject.Api.ApiException;
import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.OUT.CheckoutOut;
import com.example.fproject.Enum.SubscriptionPlanType;
import com.example.fproject.Model.User;
import com.example.fproject.Service.LemonSqueezyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final LemonSqueezyService lemonSqueezyService;

    @PostMapping("/subscribe/{planType}")
    public ResponseEntity<?> createSubscriptionCheckout(@AuthenticationPrincipal User user, @PathVariable String planType) {
        String checkoutUrl = lemonSqueezyService.createSubscriptionCheckout(planType, user.getId());
        Double amount = resolvePlanAmount(planType);
        return ResponseEntity.status(200).body(new CheckoutOut(checkoutUrl, planType, amount));
    }

    private Double resolvePlanAmount(String planType) {
        try {
            return SubscriptionPlanType.valueOf(planType.trim().toUpperCase()).getPrice();
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/subscription/{storeOwnerId}")
    public ResponseEntity<?> getSubscription(@AuthenticationPrincipal User user, @PathVariable Integer storeOwnerId) {
        if (!storeOwnerId.equals(user.getId()))
            throw new ApiException("You do not have permission to access this resource");
        return ResponseEntity.status(200).body(lemonSqueezyService.getSubscription(storeOwnerId));
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestHeader HttpHeaders headers, @RequestBody String rawBody) {
        lemonSqueezyService.processWebhook(headers, rawBody);
        return ResponseEntity.status(200).body(new ApiResponse("Webhook processed successfully"));
    }
}