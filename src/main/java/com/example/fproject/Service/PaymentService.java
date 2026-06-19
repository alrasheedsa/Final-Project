package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.OUT.PaymentOut;
import com.example.fproject.Enum.PaymentStatus;
import com.example.fproject.Model.Payment;
import com.example.fproject.Model.Subscription;
import com.example.fproject.Repository.PaymentRepository;
import com.example.fproject.Repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;

    public List<PaymentOut> getAllPayments() {

        List<Payment> payments = paymentRepository.findAll();
        List<PaymentOut> result = new ArrayList<>();

        for (Payment payment : payments) {
            result.add(mapToDTOOUT(payment));
        }

        return result;
    }

    public PaymentOut getPaymentById(Integer paymentId) {

        Payment payment = paymentRepository.findPaymentById(paymentId);

        if (payment == null) {
            throw new ApiException("Payment not found");
        }

        return mapToDTOOUT(payment);
    }

    public List<PaymentOut> getPaymentsBySubscriptionId(Integer subscriptionId) {

        Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);

        if (subscription == null) {
            throw new ApiException("Subscription not found");
        }

        List<Payment> payments = paymentRepository.findPaymentsBySubscriptionId(subscriptionId);
        List<PaymentOut> result = new ArrayList<>();

        for (Payment payment : payments) {
            result.add(mapToDTOOUT(payment));
        }

        return result;
    }

    public List<PaymentOut> getPaymentsByStatus(PaymentStatus status) {

        List<Payment> payments = paymentRepository.findPaymentsByStatus(status);
        List<PaymentOut> result = new ArrayList<>();

        for (Payment payment : payments) {
            result.add(mapToDTOOUT(payment));
        }

        return result;
    }

    public PaymentOut markPaymentAsFailed(Integer paymentId) {

        Payment payment = paymentRepository.findPaymentById(paymentId);

        if (payment == null) {
            throw new ApiException("Payment not found");
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new ApiException("Paid payment cannot be marked as failed");
        }

        if (payment.getStatus() == PaymentStatus.FAILED) {
            return mapToDTOOUT(payment);
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        return mapToDTOOUT(payment);
    }

    public void deletePayment(Integer paymentId) {

        Payment payment = paymentRepository.findPaymentById(paymentId);

        if (payment == null) {
            throw new ApiException("Payment not found");
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new ApiException("Cannot delete paid payment");
        }

        paymentRepository.delete(payment);
    }

    private PaymentOut mapToDTOOUT(Payment payment) {

        Integer subscriptionId = null;

        if (payment.getSubscription() != null) {
            subscriptionId = payment.getSubscription().getId();
        }

        return new PaymentOut(
                payment.getId(),
                payment.getAmount(),
                payment.getTransactionId(),
                payment.getPaymentProvider(),
                payment.getStatus(),
                payment.getPaidAt(),
                subscriptionId
        );
    }
}