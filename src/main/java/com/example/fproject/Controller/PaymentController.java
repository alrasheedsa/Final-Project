package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.PaymentIn;
import com.example.fproject.Service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/add/{subscriptionId}")
    public ResponseEntity<?> createPayment(@PathVariable Integer subscriptionId, @Valid @RequestBody PaymentIn dto) {
        return ResponseEntity.status(200).body(paymentService.createPayment(subscriptionId, dto));
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

    @PutMapping("/paid/{paymentId}")
    public ResponseEntity<?> markPaymentAsPaid(@PathVariable Integer paymentId, @RequestParam String transactionId) {
        return ResponseEntity.status(200).body(paymentService.markPaymentAsPaid(paymentId, transactionId));
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