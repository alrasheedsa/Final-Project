package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.StoreOwnerIn;
import com.example.fproject.DTO.OUT.StoreOwnerOut;
import com.example.fproject.Enum.RoleType;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Model.User;
import com.example.fproject.Repository.StoreOwnerRepository;
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

    public StoreOwnerOut registerStoreOwner(StoreOwnerIn dto) {

        if (userRepository.existsUserByEmail(dto.getEmail())) {
            throw new ApiException("Email already exists");
        }

        if (userRepository.existsUserByPhone(dto.getPhone())) {
            throw new ApiException("Phone already exists");
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

        storeOwnerRepository.save(storeOwner);

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

    public StoreOwnerOut getStoreOwnerByUserId(Integer userId) {
        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerByUserId(userId);

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

        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());

        userRepository.save(user);

        return mapToDTOOUT(storeOwner);
    }

    public void deleteStoreOwner(Integer storeOwnerId) {
        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);

        if (storeOwner == null) {
            throw new ApiException("Store owner not found");
        }

        User user = storeOwner.getUser();

        storeOwnerRepository.delete(storeOwner);
        userRepository.delete(user);
    }

    private StoreOwnerOut mapToDTOOUT(StoreOwner storeOwner) {
        return new StoreOwnerOut(
                storeOwner.getId(),
                storeOwner.getUser().getFullName(),
                storeOwner.getUser().getPhone(),
                storeOwner.getUser().getEmail(),
                storeOwner.getUser().getEnabled(),
                storeOwner.getUser().getCreatedAt()
        );
    }
}