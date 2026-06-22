package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.OUT.PaymentOut;
import com.example.fproject.Enum.PaymentStatus;
import com.example.fproject.Model.Payment;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Model.Subscription;
import com.example.fproject.Repository.PaymentRepository;
import com.example.fproject.Repository.StoreOwnerRepository;
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
    private final StoreOwnerRepository storeOwnerRepository;


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


    // STORE_OWNER — يجيب كل مدفوعاته عن طريق userId
    public List<PaymentOut> getPaymentsByUserId(Integer userId) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null) throw new ApiException("Store owner not found");

        List<Subscription> subscriptions = subscriptionRepository
                .findSubscriptionsByStoreOwnerId(owner.getId());

        List<PaymentOut> result = new ArrayList<>();
        for (Subscription subscription : subscriptions) {
            List<Payment> payments = paymentRepository
                    .findPaymentsBySubscriptionId(subscription.getId());
            for (Payment payment : payments) result.add(mapToDTOOUT(payment));
        }
        return result;
    }

    // STORE_OWNER — يجيب مدفوعات اشتراك معين مع تحقق الملكية
    public List<PaymentOut> getPaymentsBySubscriptionId(Integer userId, Integer subscriptionId) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null) throw new ApiException("Store owner not found");

        Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);
        if (subscription == null) throw new ApiException("Subscription not found");

        if (!subscription.getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("You do not have permission to access this subscription");

        List<Payment> payments = paymentRepository.findPaymentsBySubscriptionId(subscriptionId);
        List<PaymentOut> result = new ArrayList<>();
        for (Payment payment : payments) result.add(mapToDTOOUT(payment));
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