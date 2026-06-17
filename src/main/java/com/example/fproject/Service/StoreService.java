package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.StoreIn;
import com.example.fproject.DTO.OUT.StoreOut;
import com.example.fproject.Model.Store;
import com.example.fproject.Repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

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

    public StoreOut getStoreByStoreOwnerId(Integer storeOwnerId) {
        Store store = storeRepository.findStoreByStoreOwnerId(storeOwnerId);

        if (store == null) {
            throw new ApiException("Store not found for this store owner");
        }

        return mapToDTOOUT(store);
    }

    public StoreOut updateStore(Integer storeId, StoreIn dto) {
        Store store = storeRepository.findStoreById(storeId);

        if (store == null) {
            throw new ApiException("Store not found");
        }

        store.setName(dto.getName());
        store.setLatitude(dto.getLatitude());
        store.setLongitude(dto.getLongitude());
        store.setCampaignRadiusMeters(dto.getCampaignRadiusMeters());

        storeRepository.save(store);

        return mapToDTOOUT(store);
    }

    public void deleteStore(Integer storeId) {
        Store store = storeRepository.findStoreById(storeId);

        if (store == null) {
            throw new ApiException("Store not found");
        }

        storeRepository.delete(store);
    }

    private StoreOut mapToDTOOUT(Store store) {
        return new StoreOut(
                store.getId(),
                store.getName(),
                store.getLatitude(),
                store.getLongitude(),
                store.getStatus(),
                store.getCampaignRadiusMeters()
        );
    }
}