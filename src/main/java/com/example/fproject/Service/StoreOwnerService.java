package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.StoreOwnerIn;
import com.example.fproject.DTO.OUT.StoreOwnerOut;
import com.example.fproject.Enum.RoleType;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Model.User;
import com.example.fproject.Repository.StoreOwnerRepository;
import com.example.fproject.Repository.StoreRepository;
import com.example.fproject.Repository.SubscriptionRepository;
import com.example.fproject.Repository.UserRepository;
import jakarta.transaction.Transactional;
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
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public StoreOwnerOut registerStoreOwner(StoreOwnerIn dto) {


        if (userRepository.existsUserByEmail(dto.getEmail())) {
            throw new ApiException("Email already exists");
        }

        if (userRepository.existsUserByPhone(dto.getPhone())) {
            throw new ApiException("Phone number already exists");
        }

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setRole(RoleType.STORE_OWNER);
        user.setEnabled(false);
        userRepository.save(user);

        StoreOwner storeOwner = new StoreOwner();
        storeOwner.setUser(user);
        storeOwnerRepository.save(storeOwner);

        return mapToOut(storeOwner);

    }

    public List<StoreOwnerOut> getAllStoreOwners() {
        return storeOwnerRepository.findAll()
                .stream()
                .map(this::mapToOut)
                .toList();
    }

    public StoreOwnerOut getStoreOwnerById(Integer storeOwnerId) {
        return mapToOut(findStoreOwnerOrThrow(storeOwnerId));
    }

    @Transactional
    public StoreOwnerOut updateStoreOwner(Integer storeOwnerId, StoreOwnerIn dto) {
        StoreOwner storeOwner = findStoreOwnerOrThrow(storeOwnerId);
        User user = storeOwner.getUser();

        if (!user.getEmail().equals(dto.getEmail())
                && userRepository.existsUserByEmail(dto.getEmail())) {
            throw new ApiException("Email already exists");
        }

        if (!user.getPhone().equals(dto.getPhone())
                && userRepository.existsUserByPhone(dto.getPhone())) {
            throw new ApiException("Phone number already exists");
        }

        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());

        userRepository.save(user);

        return mapToOut(storeOwner);
    }

    @Transactional
    public void deleteStoreOwner(Integer storeOwnerId) {

        StoreOwner storeOwner = findStoreOwnerOrThrow(storeOwnerId);

        if (!storeRepository.findStoresByStoreOwnerId(storeOwnerId).isEmpty()) {
            throw new ApiException("Cannot delete store owner because it has stores");
        }

        if (!subscriptionRepository.findSubscriptionsByStoreOwnerId(storeOwnerId).isEmpty()) {
            throw new ApiException("Cannot delete store owner because it has subscriptions");
        }

        User user = storeOwner.getUser();
        storeOwnerRepository.delete(storeOwner);
        userRepository.delete(user);
    }

    private StoreOwner findStoreOwnerOrThrow(Integer storeOwnerId) {
        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);
        if (storeOwner == null) {
            throw new ApiException("Store owner not found");
        }
        return storeOwner;
    }

    private StoreOwnerOut mapToOut(StoreOwner storeOwner) {
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