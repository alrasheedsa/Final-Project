package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.StoreIn;
import com.example.fproject.DTO.OUT.StoreOut;
import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Enum.SubscriptionStatus;
import com.example.fproject.Model.Store;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Model.Subscription;
import com.example.fproject.Repository.BranchRepository;
import com.example.fproject.Repository.StoreOwnerRepository;
import com.example.fproject.Repository.StoreRepository;
import com.example.fproject.Repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final BranchRepository branchRepository;
    private final WathqService wathqService;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public StoreOut addStore(Integer storeOwnerId, StoreIn dto) {

        StoreOwner storeOwner = findStoreOwnerOrThrow(storeOwnerId);

        Subscription subscription = getActiveOrPendingSubscription(storeOwnerId);

        long currentCount = countActiveOrPendingStores(storeOwnerId);
        int maxStores = subscription.getPlanType().getMaxStores();

        if (currentCount >= maxStores) {
            throw new ApiException(
                    "Your subscription plan allows only " + maxStores + " store(s). " +
                            "Deactivate an existing store or upgrade your plan."
            );
        }

        if (storeRepository.existsStoreByCommercialRegisterNo(dto.getCommercialRegisterNo())) {
            throw new ApiException("Commercial register number is already used by another store");
        }

        if (storeRepository.existsStoreByNameAndStoreOwnerId(dto.getName(), storeOwnerId)) {
            throw new ApiException("You already have a store with this name");
        }

        wathqService.validateCommercialRegistration(dto.getCommercialRegisterNo());

        Store store = new Store();
        store.setName(dto.getName());
        store.setBusinessType(dto.getBusinessType());
        store.setCommercialRegisterNo(dto.getCommercialRegisterNo());
        store.setCommercialRegisterVerified(true);
        store.setStoreOwner(storeOwner);

        store.setStatus(resolveStoreStatus(subscription));

        storeRepository.save(store);

        return mapToOut(store);
    }

    public List<StoreOut> getAllStores() {
        return storeRepository.findAll()
                .stream()
                .map(this::mapToOut)
                .toList();
    }

    public StoreOut getStoreById(Integer storeId) {
        return mapToOut(findStoreOrThrow(storeId));
    }

    public List<StoreOut> getStoresByStoreOwnerId(Integer storeOwnerId) {
        findStoreOwnerOrThrow(storeOwnerId);
        return storeRepository.findStoresByStoreOwnerId(storeOwnerId)
                .stream()
                .map(this::mapToOut)
                .toList();
    }

    @Transactional
    public StoreOut updateStore(Integer storeId, StoreIn dto) {

        Store store = findStoreOrThrow(storeId);

        boolean nameChanged             = !store.getName().equals(dto.getName());
        boolean businessTypeChanged     = !store.getBusinessType().equals(dto.getBusinessType());
        boolean commercialRegisterChanged = !store.getCommercialRegisterNo().equals(dto.getCommercialRegisterNo());

        if (!nameChanged && !businessTypeChanged && !commercialRegisterChanged) {
            throw new ApiException("No changes detected");
        }

        if (nameChanged &&
                storeRepository.existsStoreByNameAndStoreOwnerId(dto.getName(), store.getStoreOwner().getId())) {
            throw new ApiException("You already have a store with this name");
        }

        if (commercialRegisterChanged) {

            if (storeRepository.existsStoreByCommercialRegisterNo(dto.getCommercialRegisterNo())) {
                throw new ApiException("Commercial register number is already used by another store");
            }

            wathqService.validateCommercialRegistration(dto.getCommercialRegisterNo());

            store.setCommercialRegisterNo(dto.getCommercialRegisterNo());
            store.setCommercialRegisterVerified(true);

            if (hasActiveSubscription(store.getStoreOwner().getId())) {
                store.setStatus(StoreStatus.ACTIVE);
            } else {
                store.setStatus(StoreStatus.PENDING);
            }
        }

        if (nameChanged) {
            store.setName(dto.getName());
        }

        if (businessTypeChanged) {
            store.setBusinessType(dto.getBusinessType());
        }

        storeRepository.save(store);

        return mapToOut(store);
    }

    @Transactional
    public StoreOut activateStore(Integer storeId) {

        Store store = findStoreOrThrow(storeId);

        if (store.getStatus() == StoreStatus.ACTIVE) {
            throw new ApiException("Store is already active");
        }

        if (!Boolean.TRUE.equals(store.getCommercialRegisterVerified())) {
            throw new ApiException("Store cannot be activated: commercial register is not verified");
        }

        Subscription activeSubscription = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        store.getStoreOwner().getId(),
                        SubscriptionStatus.ACTIVE
                );

        if (activeSubscription == null) {
            throw new ApiException("Store cannot be activated: no active subscription found");
        }

        if (activeSubscription.getEndDate().isBefore(LocalDate.now())) {
            throw new ApiException("Store cannot be activated: subscription has expired");
        }

        store.setStatus(StoreStatus.ACTIVE);
        storeRepository.save(store);

        return mapToOut(store);
    }

    @Transactional
    public StoreOut deactivateStore(Integer storeId) {

        Store store = findStoreOrThrow(storeId);

        if (store.getStatus() == StoreStatus.INACTIVE) {
            throw new ApiException("Store is already inactive");
        }

        store.setStatus(StoreStatus.INACTIVE);
        storeRepository.save(store);

        return mapToOut(store);
    }

    @Transactional
    public void deleteStore(Integer storeId) {

        Store store = findStoreOrThrow(storeId);

        if (store.getStatus() == StoreStatus.ACTIVE) {
            throw new ApiException("Cannot delete an active store. Deactivate it first");
        }

        if (branchRepository.existsByStoreId(storeId)) {
            throw new ApiException("Cannot delete store because it has branches");
        }

        storeRepository.delete(store);
    }

    private Store findStoreOrThrow(Integer storeId) {
        Store store = storeRepository.findStoreById(storeId);
        if (store == null) {
            throw new ApiException("Store not found");
        }
        return store;
    }

    private StoreOwner findStoreOwnerOrThrow(Integer storeOwnerId) {
        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);
        if (storeOwner == null) {
            throw new ApiException("Store owner not found");
        }
        return storeOwner;
    }


    private Subscription getActiveOrPendingSubscription(Integer storeOwnerId) {

        Subscription active = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(storeOwnerId, SubscriptionStatus.ACTIVE);

        if (active != null && !active.getEndDate().isBefore(LocalDate.now())) {
            return active;
        }

        Subscription pending = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(storeOwnerId, SubscriptionStatus.PENDING);

        if (pending != null) {
            return pending;
        }

        throw new ApiException("Store owner must choose a subscription plan before adding stores");
    }

    private boolean hasActiveSubscription(Integer storeOwnerId) {

        Subscription active = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(storeOwnerId, SubscriptionStatus.ACTIVE);

        return active != null && !active.getEndDate().isBefore(LocalDate.now());
    }


    private long countActiveOrPendingStores(Integer storeOwnerId) {
        return storeRepository.findStoresByStoreOwnerId(storeOwnerId)
                .stream()
                .filter(s -> s.getStatus() == StoreStatus.ACTIVE
                        || s.getStatus() == StoreStatus.PENDING)
                .count();
    }


    private StoreStatus resolveStoreStatus(Subscription subscription) {
        return subscription.getStatus() == SubscriptionStatus.ACTIVE
                ? StoreStatus.ACTIVE
                : StoreStatus.PENDING;
    }

    private StoreOut mapToOut(Store store) {
        return new StoreOut(
                store.getId(),
                store.getName(),
                store.getBusinessType(),
                store.getCommercialRegisterNo(),
                store.getCommercialRegisterVerified(),
                store.getStatus(),
                store.getStoreOwner().getId(),
                store.getStoreOwner().getUser().getFullName()
        );
    }
}