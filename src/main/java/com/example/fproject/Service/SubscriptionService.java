package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.SubscriptionIn;
import com.example.fproject.DTO.OUT.SubscriptionOut;
import com.example.fproject.Enum.SubscriptionPlanType;
import com.example.fproject.Enum.SubscriptionStatus;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Model.Subscription;
import com.example.fproject.Repository.StoreOwnerRepository;
import com.example.fproject.Repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final StoreOwnerRepository storeOwnerRepository;

    public SubscriptionOut createSubscription(Integer storeOwnerId, SubscriptionIn dto) {
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

        LocalDate startDate = LocalDate.now();
        LocalDate endDate;

        if (dto.getPlanType() == SubscriptionPlanType.MONTHLY) {
            endDate = startDate.plusMonths(1);
        } else if (dto.getPlanType() == SubscriptionPlanType.YEARLY) {
            endDate = startDate.plusYears(1);
        } else {
            throw new ApiException("Invalid subscription plan type");
        }

        Subscription subscription = new Subscription();
        subscription.setPlanType(dto.getPlanType());
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setStoreOwner(storeOwner);

        subscriptionRepository.save(subscription);

        return mapToDTOOUT(subscription);
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

    public SubscriptionOut updateSubscription(Integer subscriptionId, SubscriptionIn dto) {
        Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);

        if (subscription == null) {
            throw new ApiException("Subscription not found");
        }

        LocalDate startDate = subscription.getStartDate();
        LocalDate endDate;

        if (dto.getPlanType() == SubscriptionPlanType.MONTHLY) {
            endDate = startDate.plusMonths(1);
        } else if (dto.getPlanType() == SubscriptionPlanType.YEARLY) {
            endDate = startDate.plusYears(1);
        } else {
            throw new ApiException("Invalid subscription plan type");
        }

        subscription.setPlanType(dto.getPlanType());
        subscription.setEndDate(endDate);

        subscriptionRepository.save(subscription);

        return mapToDTOOUT(subscription);
    }

    public void deleteSubscription(Integer subscriptionId) {
        Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);

        if (subscription == null) {
            throw new ApiException("Subscription not found");
        }

        subscriptionRepository.delete(subscription);
    }

    public SubscriptionOut activateSubscription(Integer subscriptionId) {
        Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);

        if (subscription == null) {
            throw new ApiException("Subscription not found");
        }

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        return mapToDTOOUT(subscription);
    }

    public SubscriptionOut cancelSubscription(Integer subscriptionId) {
        Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);

        if (subscription == null) {
            throw new ApiException("Subscription not found");
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