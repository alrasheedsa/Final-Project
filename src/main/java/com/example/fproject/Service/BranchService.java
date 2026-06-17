package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.BranchIn;
import com.example.fproject.DTO.OUT.BranchOut;
import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Model.Branch;
import com.example.fproject.Model.Store;
import com.example.fproject.Repository.BranchRepository;
import com.example.fproject.Repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final StoreRepository storeRepository;

    public BranchOut addBranch(Integer storeId, BranchIn dto) {
        Store store = storeRepository.findStoreById(storeId);

        if (store == null) {
            throw new ApiException("Store not found");
        }

        if (branchRepository.existsBranchByNameAndStoreId(dto.getName(), storeId)) {
            throw new ApiException("Branch name already exists for this store");
        }

        Branch branch = new Branch();
        branch.setName(dto.getName());
        branch.setLatitude(dto.getLatitude());
        branch.setLongitude(dto.getLongitude());
        branch.setCampaignRadiusMeters(dto.getCampaignRadiusMeters());
        branch.setStatus(StoreStatus.PENDING);
        branch.setStore(store);

        branchRepository.save(branch);

        return mapToDTOOUT(branch);
    }

    public List<BranchOut> getAllBranches() {
        List<Branch> branches = branchRepository.findAll();
        List<BranchOut> result = new ArrayList<>();

        for (Branch branch : branches) {
            result.add(mapToDTOOUT(branch));
        }

        return result;
    }

    public BranchOut getBranchById(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        return mapToDTOOUT(branch);
    }

    public List<BranchOut> getBranchesByStoreId(Integer storeId) {
        Store store = storeRepository.findStoreById(storeId);

        if (store == null) {
            throw new ApiException("Store not found");
        }

        List<Branch> branches = branchRepository.findBranchesByStoreId(storeId);
        List<BranchOut> result = new ArrayList<>();

        for (Branch branch : branches) {
            result.add(mapToDTOOUT(branch));
        }

        return result;
    }

    public BranchOut updateBranch(Integer branchId, BranchIn dto) {
        Branch branch = branchRepository.findBranchById(branchId);

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        if (!branch.getName().equals(dto.getName())
                && branchRepository.existsBranchByNameAndStoreId(dto.getName(), branch.getStore().getId())) {
            throw new ApiException("Branch name already exists for this store");
        }

        branch.setName(dto.getName());
        branch.setLatitude(dto.getLatitude());
        branch.setLongitude(dto.getLongitude());
        branch.setCampaignRadiusMeters(dto.getCampaignRadiusMeters());

        branchRepository.save(branch);

        return mapToDTOOUT(branch);
    }

    public void deleteBranch(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        branchRepository.delete(branch);
    }

    public BranchOut activateBranch(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        branch.setStatus(StoreStatus.ACTIVE);
        branchRepository.save(branch);

        return mapToDTOOUT(branch);
    }

    public BranchOut deactivateBranch(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        branch.setStatus(StoreStatus.INACTIVE);
        branchRepository.save(branch);

        return mapToDTOOUT(branch);
    }

    private BranchOut mapToDTOOUT(Branch branch) {
        return new BranchOut(
                branch.getId(),
                branch.getName(),
                branch.getLatitude(),
                branch.getLongitude(),
                branch.getStatus(),
                branch.getCampaignRadiusMeters(),
                branch.getStore().getId(),
                branch.getStore().getName()
        );
    }
}