package com.example.fproject.Controller;

import com.example.fproject.Model.Payment;
import com.example.fproject.Repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final PaymentRepository paymentRepository;

    @Value("${moyasar.publishable-key}")
    private String publishableKey;

    @Value("${app.payment.callback-url}")
    private String callbackBaseUrl;

    @GetMapping("/api/v1/payment/page/{localPaymentId}")
    public String checkoutPage(@PathVariable Integer localPaymentId, Model model) {

        Payment payment = paymentRepository.findPaymentById(localPaymentId);

        if (payment == null) {
            throw new RuntimeException("Payment not found");
        }

        Long amountInHalalas = Math.round(payment.getAmount() * 100);
        String description = "Subscription payment - " + payment.getSubscription().getPlanType();
        String callbackUrl = callbackBaseUrl + "/" + localPaymentId;

        model.addAttribute("amount", amountInHalalas);
        model.addAttribute("amountSar", payment.getAmount());
        model.addAttribute("description", description);
        model.addAttribute("publishableKey", publishableKey);
        model.addAttribute("callbackUrl", callbackUrl);
        model.addAttribute("planType", payment.getSubscription().getPlanType());

        return "checkout";
    }
}