package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.OUT.PaymentOut;
import com.example.fproject.Enum.PaymentStatus;
import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Enum.SubscriptionStatus;
import com.example.fproject.Model.Branch;
import com.example.fproject.Model.Payment;
import com.example.fproject.Model.Store;
import com.example.fproject.Model.Subscription;
import com.example.fproject.Model.User;
import com.example.fproject.Repository.BranchRepository;
import com.example.fproject.Repository.PaymentRepository;
import com.example.fproject.Repository.StoreRepository;
import com.example.fproject.Repository.SubscriptionRepository;
import com.example.fproject.Repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final MoyasarService moyasarService;

    public PaymentOut verifyMoyasarPayment(Integer localPaymentId, String moyasarPaymentId) {
        Payment payment = paymentRepository.findPaymentById(localPaymentId);

        if (payment == null) {
            throw new ApiException("Payment not found");
        }

        if (moyasarPaymentId == null || moyasarPaymentId.isBlank()) {
            throw new ApiException("Moyasar payment id is required");
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            return mapToDTOOUT(payment);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ApiException("Only pending payment can be verified");
        }

        Payment existingPayment = paymentRepository.findPaymentByTransactionId(moyasarPaymentId);

        if (existingPayment != null && !existingPayment.getId().equals(payment.getId())) {
            throw new ApiException("Moyasar payment id is already linked to another payment");
        }

        JsonNode moyasarPayment = moyasarService.getPayment(moyasarPaymentId);

        String moyasarStatus = moyasarService.extractPaymentStatus(moyasarPayment);
        Long moyasarAmount = moyasarService.extractAmount(moyasarPayment);
        String moyasarCurrency = moyasarService.extractCurrency(moyasarPayment);

        validateMoyasarAmountAndCurrency(payment, moyasarAmount, moyasarCurrency);

        payment.setTransactionId(moyasarPaymentId);

        if ("paid".equalsIgnoreCase(moyasarStatus)) {
            return markPaymentAsPaid(payment, moyasarPaymentId);
        }

        if ("failed".equalsIgnoreCase(moyasarStatus)) {
            paymentRepository.save(payment);
            return markPaymentAsFailed(payment.getId());
        }

        paymentRepository.save(payment);
        return mapToDTOOUT(payment);
    }

    public PaymentOut getAllPaymentsStatus(Integer localPaymentId) {
        Payment payment = paymentRepository.findPaymentById(localPaymentId);

        if (payment == null) {
            throw new ApiException("Payment not found");
        }

        if (payment.getTransactionId() == null || payment.getTransactionId().isBlank()) {
            throw new ApiException("Payment does not have Moyasar transaction id yet");
        }

        return verifyMoyasarPayment(localPaymentId, payment.getTransactionId());
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

    private PaymentOut markPaymentAsPaid(Payment payment, String transactionId) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            return mapToDTOOUT(payment);
        }

        Subscription subscription = payment.getSubscription();

        if (subscription.getStatus() != SubscriptionStatus.PENDING) {
            throw new ApiException("Payment subscription is not pending");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId(transactionId);
        payment.setPaidAt(LocalDateTime.now());

        paymentRepository.save(payment);

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        activateStoreOwnerAccountStoresAndBranches(subscription);

        return mapToDTOOUT(payment);
    }

    private void activateStoreOwnerAccountStoresAndBranches(Subscription subscription) {
        User user = subscription.getStoreOwner().getUser();
        user.setEnabled(true);
        userRepository.save(user);

        List<Store> stores = storeRepository.findStoresByStoreOwnerId(subscription.getStoreOwner().getId());

        for (Store store : stores) {
            if (Boolean.TRUE.equals(store.getCommercialRegisterVerified())) {
                store.setStatus(StoreStatus.ACTIVE);
                storeRepository.save(store);

                List<Branch> branches = branchRepository.findBranchesByStoreId(store.getId());

                for (Branch branch : branches) {
                    branch.setStatus(StoreStatus.ACTIVE);
                    branchRepository.save(branch);
                }
            }
        }
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

    private void validateMoyasarAmountAndCurrency(Payment payment, Long moyasarAmount, String moyasarCurrency) {
        Long localAmountInHalalas = Math.round(payment.getAmount() * 100);

        if (!localAmountInHalalas.equals(moyasarAmount)) {
            throw new ApiException("Moyasar amount does not match local payment amount");
        }

        if (!"SAR".equalsIgnoreCase(moyasarCurrency)) {
            throw new ApiException("Moyasar currency does not match local payment currency");
        }
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