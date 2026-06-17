package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.StoreOwnerIn;
import com.example.fproject.DTO.OUT.StoreOut;
import com.example.fproject.DTO.OUT.StoreOwnerOut;
import com.example.fproject.Enum.RoleType;
import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Model.Store;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Model.User;
import com.example.fproject.Repository.StoreOwnerRepository;
import com.example.fproject.Repository.StoreRepository;
import com.example.fproject.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreOwnerService {

    private final StoreOwnerRepository storeOwnerRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    public StoreOwnerOut registerStoreOwner(StoreOwnerIn dto) {

        if (userRepository.existsUserByEmail(dto.getEmail())) {
            throw new ApiException("Email already exists");
        }

        if (userRepository.existsUserByPhone(dto.getPhone())) {
            throw new ApiException("Phone already exists");
        }

        if (storeOwnerRepository.existsStoreOwnerByCommercialRegisterNo(dto.getCommercialRegisterNo())) {
            throw new ApiException("Commercial register number already exists");
        }

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setRole(RoleType.STORE_OWNER);
        user.setEnabled(true);

        userRepository.save(user);

        StoreOwner storeOwner = new StoreOwner();
        storeOwner.setUser(user);
        storeOwner.setCommercialRegisterNo(dto.getCommercialRegisterNo());
        storeOwner.setCommercialRegisterVerified(false);
        storeOwner.setAccountStatus(StoreStatus.PENDING);
        storeOwner.setBusinessType(dto.getBusinessType());
        storeOwner.setCampaignRadiusMeters(dto.getCampaignRadiusMeters());

        storeOwnerRepository.save(storeOwner);

        Store store = new Store();
        store.setName(dto.getStoreName());
        store.setLatitude(dto.getLatitude());
        store.setLongitude(dto.getLongitude());
        store.setStatus(StoreStatus.PENDING);
        store.setCampaignRadiusMeters(dto.getCampaignRadiusMeters());
        store.setStoreOwner(storeOwner);

        storeRepository.save(store);

        return mapToDTOOUT(storeOwner);
    }

    public List<StoreOwnerOut> getAllStoreOwners() {
        List<StoreOwner> storeOwners = storeOwnerRepository.findAll();
        List<StoreOwnerOut> result = new ArrayList<>();

        for (StoreOwner storeOwner : storeOwners) {
            result.add(mapToDTOOUT(storeOwner));
        }

        return result;
    }

    public StoreOwnerOut getStoreOwnerById(Integer storeOwnerId) {
        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);

        if (storeOwner == null) {
            throw new ApiException("Store owner not found");
        }

        return mapToDTOOUT(storeOwner);
    }

    public StoreOwnerOut updateStoreOwner(Integer storeOwnerId, StoreOwnerIn dto) {
        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);

        if (storeOwner == null) {
            throw new ApiException("Store owner not found");
        }

        User user = storeOwner.getUser();

        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsUserByEmail(dto.getEmail())) {
            throw new ApiException("Email already exists");
        }

        if (!user.getPhone().equals(dto.getPhone()) && userRepository.existsUserByPhone(dto.getPhone())) {
            throw new ApiException("Phone already exists");
        }

        if (!storeOwner.getCommercialRegisterNo().equals(dto.getCommercialRegisterNo())
                && storeOwnerRepository.existsStoreOwnerByCommercialRegisterNo(dto.getCommercialRegisterNo())) {
            throw new ApiException("Commercial register number already exists");
        }

        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());

        userRepository.save(user);

        storeOwner.setCommercialRegisterNo(dto.getCommercialRegisterNo());
        storeOwner.setBusinessType(dto.getBusinessType());
        storeOwner.setCampaignRadiusMeters(dto.getCampaignRadiusMeters());

        storeOwnerRepository.save(storeOwner);

        Store store = storeRepository.findStoreByStoreOwnerId(storeOwnerId);

        if (store != null) {
            store.setName(dto.getStoreName());
            store.setLatitude(dto.getLatitude());
            store.setLongitude(dto.getLongitude());
            store.setCampaignRadiusMeters(dto.getCampaignRadiusMeters());

            storeRepository.save(store);
        }

        return mapToDTOOUT(storeOwner);
    }

    public void deleteStoreOwner(Integer storeOwnerId) {
        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);

        if (storeOwner == null) {
            throw new ApiException("Store owner not found");
        }

        User user = storeOwner.getUser();

        Store store = storeRepository.findStoreByStoreOwnerId(storeOwnerId);
        if (store != null) {
            storeRepository.delete(store);
        }

        storeOwnerRepository.delete(storeOwner);
        userRepository.delete(user);
    }

    private StoreOwnerOut mapToDTOOUT(StoreOwner storeOwner) {
        Store store = storeRepository.findStoreByStoreOwnerId(storeOwner.getId());

        StoreOut storeDTOOUT = null;

        if (store != null) {
            storeDTOOUT = new StoreOut(
                    store.getId(),
                    store.getName(),
                    store.getLatitude(),
                    store.getLongitude(),
                    store.getStatus(),
                    store.getCampaignRadiusMeters()
            );
        }

        return new StoreOwnerOut(
                storeOwner.getId(),
                storeOwner.getUser().getFullName(),
                storeOwner.getUser().getPhone(),
                storeOwner.getUser().getEmail(),
                storeOwner.getUser().getEnabled(),
                storeOwner.getUser().getCreatedAt(),
                storeOwner.getCommercialRegisterNo(),
                storeOwner.getCommercialRegisterVerified(),
                storeOwner.getAccountStatus(),
                storeOwner.getBusinessType(),
                storeOwner.getCampaignRadiusMeters(),
                storeDTOOUT
        );
    }
}