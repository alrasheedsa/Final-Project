package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.StoreIn;
import com.example.fproject.DTO.OUT.StoreOut;
import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Model.Store;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Repository.BranchRepository;
import com.example.fproject.Repository.StoreOwnerRepository;
import com.example.fproject.Repository.StoreRepository;
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

        Store store = new Store();
        store.setName(dto.getName());
        store.setBusinessType(dto.getBusinessType());
        store.setCommercialRegisterNo(dto.getCommercialRegisterNo());
        store.setCommercialRegisterVerified(false);
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

        if (!store.getCommercialRegisterNo().equals(dto.getCommercialRegisterNo())
                && storeRepository.existsStoreByCommercialRegisterNo(dto.getCommercialRegisterNo())) {
            throw new ApiException("Commercial register number already exists");
        }

        if (!store.getName().equals(dto.getName())
                && storeRepository.existsStoreByNameAndStoreOwnerId(dto.getName(), store.getStoreOwner().getId())) {
            throw new ApiException("Store name already exists for this store owner");
        }

        store.setName(dto.getName());
        store.setBusinessType(dto.getBusinessType());
        store.setCommercialRegisterNo(dto.getCommercialRegisterNo());

        storeRepository.save(store);

        return mapToDTOOUT(store);
    }

    public StoreOut activateStore(Integer storeId) {
        Store store = storeRepository.findStoreById(storeId);

        if (store == null) {
            throw new ApiException("Store not found");
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
