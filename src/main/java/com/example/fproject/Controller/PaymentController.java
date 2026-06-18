package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.Service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/callback/{localPaymentId}")
    public ResponseEntity<?> paymentCallback(
            @PathVariable Integer localPaymentId,
            @RequestParam String id) {

        return ResponseEntity.status(200).body(
                paymentService.verifyMoyasarPayment(localPaymentId, id)
        );
    }

    @GetMapping("/verify/{localPaymentId}")
    public ResponseEntity<?> verifyPayment(
            @PathVariable Integer localPaymentId,
            @RequestParam String id) {

        return ResponseEntity.status(200).body(
                paymentService.verifyMoyasarPayment(localPaymentId, id)
        );
    }

    @GetMapping("/get-status/{localPaymentId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable Integer localPaymentId) {
        return ResponseEntity.status(200).body(paymentService.getAllPaymentsStatus(localPaymentId));
    }

    @GetMapping("/get")
    public ResponseEntity<?> getAllPayments() {
        return ResponseEntity.status(200).body(paymentService.getAllPayments());
    }

    @GetMapping("/get/{paymentId}")
    public ResponseEntity<?> getPaymentById(@PathVariable Integer paymentId) {
        return ResponseEntity.status(200).body(paymentService.getPaymentById(paymentId));
    }

    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<?> getPaymentsBySubscriptionId(@PathVariable Integer subscriptionId) {
        return ResponseEntity.status(200).body(paymentService.getPaymentsBySubscriptionId(subscriptionId));
    }

    @PutMapping("/failed/{paymentId}")
    public ResponseEntity<?> markPaymentAsFailed(@PathVariable Integer paymentId) {
        return ResponseEntity.status(200).body(paymentService.markPaymentAsFailed(paymentId));
    }

    @DeleteMapping("/delete/{paymentId}")
    public ResponseEntity<?> deletePayment(@PathVariable Integer paymentId) {
        paymentService.deletePayment(paymentId);
        return ResponseEntity.status(200).body(new ApiResponse("Payment deleted successfully"));
    }
}