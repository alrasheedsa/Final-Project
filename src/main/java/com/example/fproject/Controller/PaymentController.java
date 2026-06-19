package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.Service.LemonSqueezyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final LemonSqueezyService lemonSqueezyService;

    @PostMapping("/subscribe/{planType}/{storeOwnerId}")
    public ResponseEntity<?> createSubscriptionCheckout(@PathVariable String planType, @PathVariable Integer storeOwnerId) {
        String checkoutUrl = lemonSqueezyService.createSubscriptionCheckout(planType, storeOwnerId);
        return ResponseEntity.status(200).body(Map.of("message", checkoutUrl));
    }

    @GetMapping("/subscription/{storeOwnerId}")
    public ResponseEntity<?> getSubscription(@PathVariable Integer storeOwnerId) {
        return ResponseEntity.status(200).body(lemonSqueezyService.getSubscription(storeOwnerId));
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestHeader HttpHeaders headers, @RequestBody String rawBody) {
        lemonSqueezyService.processWebhook(headers, rawBody);
        return ResponseEntity.status(200).body(new ApiResponse("Webhook processed successfully"));
    }
}