package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.OUT.CheckoutOut;
import com.example.fproject.Enum.SubscriptionPlanType;
import com.example.fproject.Model.User;
import com.example.fproject.Service.LemonSqueezyService;
import com.example.fproject.Service.PaymentService;
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
    private final PaymentService paymentService;

    @PostMapping("/subscribe/{planType}")
    public ResponseEntity<?> createSubscriptionCheckout(@AuthenticationPrincipal User user,
                                                        @PathVariable String planType) {
        String checkoutUrl = lemonSqueezyService.createSubscriptionCheckout(planType, user.getId());
        Double amount = resolvePlanAmount(planType);
        return ResponseEntity.status(200).body(new CheckoutOut(checkoutUrl, planType, amount));
    }

    @GetMapping("/subscription")
    public ResponseEntity<?> getSubscription(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(lemonSqueezyService.getSubscription(user.getId()));
    }

    @GetMapping("/my-payments")
    public ResponseEntity<?> getMyPayments(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(paymentService.getPaymentsByUserId(user.getId()));
    }

    @GetMapping("/my-payments/subscription/{subscriptionId}")
    public ResponseEntity<?> getMyPaymentsBySubscription(@AuthenticationPrincipal User user,
                                                          @PathVariable Integer subscriptionId) {
        return ResponseEntity.status(200).body(
                paymentService.getPaymentsBySubscriptionId(user.getId(), subscriptionId));
    }

    @GetMapping("/get")
    public ResponseEntity<?> getAllPayments() {
        return ResponseEntity.status(200).body(paymentService.getAllPayments());
    }

    @GetMapping("/get/{paymentId}")
    public ResponseEntity<?> getPaymentById(@PathVariable Integer paymentId) {
        return ResponseEntity.status(200).body(paymentService.getPaymentById(paymentId));
    }

    @PutMapping("/mark-failed/{paymentId}")
    public ResponseEntity<?> markPaymentAsFailed(@PathVariable Integer paymentId) {
        return ResponseEntity.status(200).body(paymentService.markPaymentAsFailed(paymentId));
    }

    @DeleteMapping("/delete/{paymentId}")
    public ResponseEntity<?> deletePayment(@PathVariable Integer paymentId) {
        paymentService.deletePayment(paymentId);
        return ResponseEntity.status(200).body(new ApiResponse("Payment deleted successfully"));
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestHeader HttpHeaders headers, @RequestBody String rawBody) {
        lemonSqueezyService.processWebhook(headers, rawBody);
        return ResponseEntity.status(200).body(new ApiResponse("Webhook processed successfully"));
    }

    private Double resolvePlanAmount(String planType) {
        try {
            return SubscriptionPlanType.valueOf(planType.trim().toUpperCase()).getPrice();
        } catch (Exception e) {
            return null;
        }
    }
}