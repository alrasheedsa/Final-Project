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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final BranchRepository branchRepository;
    private final WathqService wathqService;
    private final SubscriptionRepository subscriptionRepository;

    public StoreOut addStore(Integer storeOwnerId, StoreIn dto) {
        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);

        if (storeOwner == null) {
            throw new ApiException("Store owner not found");
        }

        if (storeRepository.existsStoreByCommercialRegisterNo(dto.getCommercialRegisterNo())) {
            throw new ApiException("Commercial register number already exists");
        }

        if (storeRepository.existsStoreByNameAndStoreOwnerId(dto.getName(), storeOwnerId)) {
            throw new ApiException("Store name already exists for this store owner");
        }

        wathqService.validateCommercialRegistration(dto.getCommercialRegisterNo());

        Store store = new Store();
        store.setName(dto.getName());
        store.setBusinessType(dto.getBusinessType());
        store.setCommercialRegisterNo(dto.getCommercialRegisterNo());
        store.setCommercialRegisterVerified(true);
        store.setStatus(StoreStatus.PENDING);
        store.setStoreOwner(storeOwner);

        storeRepository.save(store);

        return mapToDTOOUT(store);
    }

    public List<StoreOut> getAllStores() {
        List<Store> stores = storeRepository.findAll();
        List<StoreOut> result = new ArrayList<>();

        for (Store store : stores) {
            result.add(mapToDTOOUT(store));
        }

        return result;
    }

    public StoreOut getStoreById(Integer storeId) {
        Store store = storeRepository.findStoreById(storeId);

        if (store == null) {
            throw new ApiException("Store not found");
        }

        return mapToDTOOUT(store);
    }

    public List<StoreOut> getStoresByStoreOwnerId(Integer storeOwnerId) {
        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);

        if (storeOwner == null) {
            throw new ApiException("Store owner not found");
        }

        List<Store> stores = storeRepository.findStoresByStoreOwnerId(storeOwnerId);
        List<StoreOut> result = new ArrayList<>();

        for (Store store : stores) {
            result.add(mapToDTOOUT(store));
        }

        return result;
    }

    public StoreOut updateStore(Integer storeId, StoreIn dto) {
        Store store = storeRepository.findStoreById(storeId);

        if (store == null) {
            throw new ApiException("Store not found");
        }

        boolean nameChanged = !store.getName().equals(dto.getName());
        boolean businessTypeChanged = !store.getBusinessType().equals(dto.getBusinessType());
        boolean commercialRegisterChanged =
                !store.getCommercialRegisterNo().equals(dto.getCommercialRegisterNo());

        if (!nameChanged && !businessTypeChanged && !commercialRegisterChanged) {
            throw new ApiException("No changes detected");
        }

        if (commercialRegisterChanged
                && storeRepository.existsStoreByCommercialRegisterNo(dto.getCommercialRegisterNo())) {
            throw new ApiException("Commercial register number already exists");
        }

        if (nameChanged
                && storeRepository.existsStoreByNameAndStoreOwnerId(dto.getName(), store.getStoreOwner().getId())) {
            throw new ApiException("Store name already exists for this store owner");
        }

        if (commercialRegisterChanged) {
            wathqService.validateCommercialRegistration(dto.getCommercialRegisterNo());

            store.setCommercialRegisterNo(dto.getCommercialRegisterNo());
            store.setCommercialRegisterVerified(true);

            boolean hasActiveSubscription = hasActiveSubscription(store.getStoreOwner().getId());

            if (hasActiveSubscription) {
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

        return mapToDTOOUT(store);
    }

    private boolean hasActiveSubscription(Integer storeOwnerId) {
        Subscription activeSubscription =
                subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        storeOwnerId,
                        SubscriptionStatus.ACTIVE
                );

        return activeSubscription != null
                && !activeSubscription.getEndDate().isBefore(java.time.LocalDate.now());
    }

    public StoreOut activateStore(Integer storeId) {
        Store store = storeRepository.findStoreById(storeId);

        if (store == null) {
            throw new ApiException("Store not found");
        }

        if (!Boolean.TRUE.equals(store.getCommercialRegisterVerified())) {
            throw new ApiException("Store cannot be activated before commercial register verification");
        }

        Subscription activeSubscription =
                subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        store.getStoreOwner().getId(),
                        SubscriptionStatus.ACTIVE
                );

        if (activeSubscription == null) {
            throw new ApiException("Store cannot be activated before active subscription payment");
        }

        if (activeSubscription.getEndDate().isBefore(java.time.LocalDate.now())) {
            throw new ApiException("Store cannot be activated because subscription is expired");
        }

        store.setStatus(StoreStatus.ACTIVE);
        storeRepository.save(store);

        return mapToDTOOUT(store);
    }

    public StoreOut deactivateStore(Integer storeId) {
        Store store = storeRepository.findStoreById(storeId);

        if (store == null) {
            throw new ApiException("Store not found");
        }

        store.setStatus(StoreStatus.INACTIVE);
        storeRepository.save(store);

        return mapToDTOOUT(store);
    }

    public void deleteStore(Integer storeId) {
        Store store = storeRepository.findStoreById(storeId);

        if (store == null) {
            throw new ApiException("Store not found");
        }

        if (branchRepository.existsByStoreId(storeId)) {
            throw new ApiException("Cannot delete store because it has branches");
        }

        storeRepository.delete(store);
    }

    private StoreOut mapToDTOOUT(Store store) {
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
