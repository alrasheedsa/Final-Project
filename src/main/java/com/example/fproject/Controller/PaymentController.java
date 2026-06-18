package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.OUT.PaymentOut;
import com.example.fproject.Service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/callback/{localPaymentId}")
    public String paymentCallback(
            @PathVariable Integer localPaymentId,
            @RequestParam(required = false) String id,
            Model model) {

        if (id == null || id.isBlank()) {
            model.addAttribute("status", "FAILED");
            model.addAttribute("message", "Moyasar payment id is missing");
            return "result";
        }

        PaymentOut paymentResult = paymentService.verifyMoyasarPayment(localPaymentId, id);

        model.addAttribute("paymentId", paymentResult.getTransactionId());
        model.addAttribute("localPaymentId", paymentResult.getId());
        model.addAttribute("status", paymentResult.getStatus());
        model.addAttribute("amount", paymentResult.getAmount());
        model.addAttribute("currency", "SAR");

        return "result";
    }

    @ResponseBody
    @GetMapping("/verify/{localPaymentId}")
    public ResponseEntity<?> verifyPayment(
            @PathVariable Integer localPaymentId,
            @RequestParam String id) {

        return ResponseEntity.status(200).body(
                paymentService.verifyMoyasarPayment(localPaymentId, id)
        );
    }

    @ResponseBody
    @GetMapping("/get-status/{localPaymentId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable Integer localPaymentId) {
        return ResponseEntity.status(200).body(paymentService.getAllPaymentsStatus(localPaymentId));
    }

    @ResponseBody
    @GetMapping("/get")
    public ResponseEntity<?> getAllPayments() {
        return ResponseEntity.status(200).body(paymentService.getAllPayments());
    }

    @ResponseBody
    @GetMapping("/get/{paymentId}")
    public ResponseEntity<?> getPaymentById(@PathVariable Integer paymentId) {
        return ResponseEntity.status(200).body(paymentService.getPaymentById(paymentId));
    }

    @ResponseBody
    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<?> getPaymentsBySubscriptionId(@PathVariable Integer subscriptionId) {
        return ResponseEntity.status(200).body(paymentService.getPaymentsBySubscriptionId(subscriptionId));
    }

    @ResponseBody
    @PutMapping("/failed/{paymentId}")
    public ResponseEntity<?> markPaymentAsFailed(@PathVariable Integer paymentId) {
        return ResponseEntity.status(200).body(paymentService.markPaymentAsFailed(paymentId));
    }

    @ResponseBody
    @DeleteMapping("/delete/{paymentId}")
    public ResponseEntity<?> deletePayment(@PathVariable Integer paymentId) {
        paymentService.deletePayment(paymentId);
        return ResponseEntity.status(200).body(new ApiResponse("Payment deleted successfully"));
    }
}