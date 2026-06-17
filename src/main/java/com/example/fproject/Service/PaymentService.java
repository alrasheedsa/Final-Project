package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.PaymentIn;
import com.example.fproject.DTO.OUT.PaymentOut;
import com.example.fproject.Enum.PaymentStatus;
import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Enum.SubscriptionStatus;
import com.example.fproject.Model.Payment;
import com.example.fproject.Model.Store;
import com.example.fproject.Model.Subscription;
import com.example.fproject.Repository.PaymentRepository;
import com.example.fproject.Repository.StoreRepository;
import com.example.fproject.Repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final StoreRepository storeRepository;

    public PaymentOut createPayment(Integer subscriptionId, PaymentIn dto) {
        Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);

        if (subscription == null) {
            throw new ApiException("Subscription not found");
        }

        if (subscription.getStatus() != SubscriptionStatus.PENDING) {
            throw new ApiException("Payment can only be created for pending subscription");
        }

        Payment payment = new Payment();
        payment.setAmount(dto.getAmount());
        payment.setPaymentProvider(dto.getPaymentProvider());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setSubscription(subscription);

        paymentRepository.save(payment);

        return mapToDTOOUT(payment);
    }

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

    public PaymentOut markPaymentAsPaid(Integer paymentId, String transactionId) {
        Payment payment = paymentRepository.findPaymentById(paymentId);

        if (payment == null) {
            throw new ApiException("Payment not found");
        }

        if (paymentRepository.existsPaymentByTransactionId(transactionId)) {
            throw new ApiException("Transaction id already exists");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId(transactionId);
        payment.setPaidAt(LocalDateTime.now());

        paymentRepository.save(payment);

        Subscription subscription = payment.getSubscription();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        Store store = storeRepository.findStoreByStoreOwnerId(subscription.getStoreOwner().getId());

        if (store != null) {
            store.setStatus(StoreStatus.ACTIVE);
            storeRepository.save(store);
        }

        subscription.getStoreOwner().setAccountStatus(StoreStatus.ACTIVE);

        return mapToDTOOUT(payment);
    }

    public PaymentOut markPaymentAsFailed(Integer paymentId) {
        Payment payment = paymentRepository.findPaymentById(paymentId);

        if (payment == null) {
            throw new ApiException("Payment not found");
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

        paymentRepository.delete(payment);
    }

    private PaymentOut mapToDTOOUT(Payment payment) {
        return new PaymentOut(
                payment.getId(),
                payment.getAmount(),
                payment.getTransactionId(),
                payment.getPaymentProvider(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getSubscription().getId()
        );
    }
}