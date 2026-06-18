package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.SubscriptionIn;
import com.example.fproject.DTO.OUT.MoyasarCheckoutOut;
import com.example.fproject.DTO.OUT.SubscriptionOut;
import com.example.fproject.Enum.PaymentStatus;
import com.example.fproject.Enum.SubscriptionPlanType;
import com.example.fproject.Enum.SubscriptionStatus;
import com.example.fproject.Model.Payment;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Model.Subscription;
import com.example.fproject.Repository.PaymentRepository;
import com.example.fproject.Repository.StoreOwnerRepository;
import com.example.fproject.Repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final PaymentRepository paymentRepository;
    private final MoyasarService moyasarService;

    @Value("${app.payment.callback-url}")
    private String paymentCallbackBaseUrl;

    public MoyasarCheckoutOut createSubscription(Integer storeOwnerId, SubscriptionIn dto) {
        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);

        if (storeOwner == null) {
            throw new ApiException("Store owner not found");
        }

        Subscription activeSubscription =
                subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        storeOwnerId,
                        SubscriptionStatus.ACTIVE
                );

        if (activeSubscription != null && !activeSubscription.getEndDate().isBefore(LocalDate.now())) {
            throw new ApiException("Store owner already has an active subscription");
        }

        Subscription pendingSubscription =
                subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        storeOwnerId,
                        SubscriptionStatus.PENDING
                );

        if (pendingSubscription != null) {
            throw new ApiException("Store owner already has a pending subscription");
        }

        SubscriptionPlanType planType = dto.getPlanType();

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(planType.getDurationMonths());

        Double amount = planType.getPrice();
        Long amountInHalalas = Math.round(amount * 100);

        Subscription subscription = new Subscription();
        subscription.setPlanType(planType);
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setStoreOwner(storeOwner);

        subscriptionRepository.save(subscription);

        try {
            Payment payment = new Payment();
            payment.setAmount(amount);
            payment.setPaymentProvider("MOYASAR");
            payment.setStatus(PaymentStatus.PENDING);
            payment.setSubscription(subscription);

            paymentRepository.save(payment);

            String description = "Subscription payment - " + planType;
            String callbackUrl = paymentCallbackBaseUrl + "/" + payment.getId();

            return new MoyasarCheckoutOut(
                    payment.getId(),
                    subscription.getId(),
                    amount,
                    amountInHalalas,
                    "SAR",
                    description,
                    moyasarService.getPublishableKey(),
                    callbackUrl,
                    "Checkout created successfully"
            );

        } catch (Exception e) {
            subscriptionRepository.delete(subscription);
            throw new ApiException("Could not create subscription checkout: " + e.getMessage());
        }
    }

    public List<SubscriptionOut> getAllSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        List<SubscriptionOut> result = new ArrayList<>();

        for (Subscription subscription : subscriptions) {
            result.add(mapToDTOOUT(subscription));
        }

        return result;
    }

    public SubscriptionOut getSubscriptionById(Integer subscriptionId) {
        Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);

        if (subscription == null) {
            throw new ApiException("Subscription not found");
        }

        return mapToDTOOUT(subscription);
    }

    public List<SubscriptionOut> getSubscriptionsByStoreOwnerId(Integer storeOwnerId) {
        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);

        if (storeOwner == null) {
            throw new ApiException("Store owner not found");
        }

        List<Subscription> subscriptions = subscriptionRepository.findSubscriptionsByStoreOwnerId(storeOwnerId);
        List<SubscriptionOut> result = new ArrayList<>();

        for (Subscription subscription : subscriptions) {
            result.add(mapToDTOOUT(subscription));
        }

        return result;
    }

    public SubscriptionOut getActiveSubscriptionByStoreOwnerId(Integer storeOwnerId) {
        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);

        if (storeOwner == null) {
            throw new ApiException("Store owner not found");
        }

        Subscription activeSubscription =
                subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        storeOwnerId,
                        SubscriptionStatus.ACTIVE
                );

        if (activeSubscription == null || activeSubscription.getEndDate().isBefore(LocalDate.now())) {
            throw new ApiException("Active subscription not found");
        }

        return mapToDTOOUT(activeSubscription);
    }

    public SubscriptionOut updateSubscription(Integer subscriptionId, SubscriptionIn dto) {
        Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);

        if (subscription == null) {
            throw new ApiException("Subscription not found");
        }

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            throw new ApiException("Cannot update active subscription");
        }

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new ApiException("Cannot update cancelled subscription");
        }

        if (!paymentRepository.findPaymentsBySubscriptionId(subscriptionId).isEmpty()) {
            throw new ApiException("Cannot update subscription after payment creation");
        }

        SubscriptionPlanType planType = dto.getPlanType();

        subscription.setPlanType(planType);
        subscription.setEndDate(subscription.getStartDate().plusMonths(planType.getDurationMonths()));

        subscriptionRepository.save(subscription);

        return mapToDTOOUT(subscription);
    }

    public void deleteSubscription(Integer subscriptionId) {
        Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);

        if (subscription == null) {
            throw new ApiException("Subscription not found");
        }

        List<Payment> payments = paymentRepository.findPaymentsBySubscriptionId(subscriptionId);

        if (!payments.isEmpty()) {
            throw new ApiException("Cannot delete subscription because it has payments");
        }

        subscriptionRepository.delete(subscription);
    }

    public SubscriptionOut cancelSubscription(Integer subscriptionId) {
        Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);

        if (subscription == null) {
            throw new ApiException("Subscription not found");
        }

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            throw new ApiException("Cannot cancel active subscription from here");
        }

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new ApiException("Subscription is already cancelled");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);

        return mapToDTOOUT(subscription);
    }

    private SubscriptionOut mapToDTOOUT(Subscription subscription) {
        return new SubscriptionOut(
                subscription.getId(),
                subscription.getPlanType(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getStatus(),
                subscription.getStoreOwner().getUser().getFullName()
        );
    }
}