package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.OUT.SubscriptionPlanOut;
import com.example.fproject.Enum.SubscriptionPlanType;
import com.example.fproject.Enum.SubscriptionStatus;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Model.Subscription;
import com.example.fproject.Repository.StoreOwnerRepository;
import com.example.fproject.Repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final LemonSqueezyService lemonSqueezyService;

    public List<SubscriptionPlanOut> getSubscriptionPlans() {
        return Arrays.stream(SubscriptionPlanType.values())
                .map(plan -> new SubscriptionPlanOut(
                        plan,
                        plan.getPrice(),
                        plan.getMaxStores(),
                        plan.getMaxBranchesPerStore(),
                        plan.getDurationMonths()
                ))
                .toList();
    }

    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    public Subscription getSubscriptionById(Integer subscriptionId) {

        Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);

        if (subscription == null) {
            throw new ApiException("Subscription not found");
        }

        return subscription;
    }

    public List<Subscription> getSubscriptionsByStoreOwner(Integer storeOwnerId) {

        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);

        if (storeOwner == null) {
            throw new ApiException("Store owner not found");
        }

        return subscriptionRepository.findSubscriptionsByStoreOwnerId(storeOwnerId);
    }

    public Subscription getActiveSubscription(Integer storeOwnerId) {

        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);

        if (storeOwner == null) {
            throw new ApiException("Store owner not found");
        }

        Subscription subscription =
                subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        storeOwnerId,
                        SubscriptionStatus.ACTIVE
                );

        if (subscription == null) {
            throw new ApiException("No active subscription found");
        }

        return subscription;
    }

    public void cancelSubscription(Integer subscriptionId) {

        Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);

        if (subscription == null) {
            throw new ApiException("Subscription not found");
        }

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new ApiException("Subscription is already cancelled");
        }

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            lemonSqueezyService.cancelLemonSubscription(subscription.getLemonSubscriptionId());
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setLemonStatus("cancelled");
        subscriptionRepository.save(subscription);
    }

    public void expireSubscription(Integer subscriptionId) {

        Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);

        if (subscription == null) {
            throw new ApiException("Subscription not found");
        }

        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(subscription);
    }
}
