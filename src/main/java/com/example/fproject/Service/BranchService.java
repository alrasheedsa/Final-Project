package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.BranchIn;
import com.example.fproject.DTO.OUT.BranchOut;
import com.example.fproject.DTO.OUT.BranchRadiusOut;
import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Enum.SubscriptionStatus;
import com.example.fproject.Model.Branch;
import com.example.fproject.Model.Customer;
import com.example.fproject.Model.Store;
import com.example.fproject.Model.Subscription;
import com.example.fproject.Repository.BranchRepository;
import com.example.fproject.Repository.CampaignRepository;
import com.example.fproject.Repository.CustomerRepository;
import com.example.fproject.Repository.MonthlyReportRepository;
import com.example.fproject.Repository.SalesRecordRepository;
import com.example.fproject.Repository.StoreRepository;
import com.example.fproject.Repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final StoreRepository storeRepository;
    private final GoogleMapService googleMapService;
    private final SalesRecordRepository salesRecordRepository;
    private final CampaignRepository campaignRepository;
    private final MonthlyReportRepository monthlyReportRepository;
    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;

    public BranchOut addBranch(Integer storeId, BranchIn dto) {
        Store store = storeRepository.findStoreById(storeId);

        if (store == null) {
            throw new ApiException("Store not found");
        }

        if (store.getStatus() == StoreStatus.INACTIVE) {
            throw new ApiException("Cannot add branch to inactive store");
        }

        if (!Boolean.TRUE.equals(store.getCommercialRegisterVerified())) {
            throw new ApiException("Cannot add branch before commercial register verification");
        }

        if (branchRepository.existsBranchByNameAndStoreId(dto.getName(), storeId)) {
            throw new ApiException("Branch name already exists for this store");
        }

        validateBranchLimit(store);

        double[] coordinates = googleMapService.extractLocationFromLink(dto.getLocationUrl());

        Branch branch = new Branch();
        branch.setName(dto.getName());
        branch.setLocationUrl(dto.getLocationUrl());
        branch.setLatitude(coordinates[0]);
        branch.setLongitude(coordinates[1]);
        branch.setCampaignRadiusMeters(dto.getCampaignRadiusMeters());
        branch.setRecommendedRadiusMeters(null);
        branch.setOpeningTime(dto.getOpeningTime());
        branch.setClosingTime(dto.getClosingTime());

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

        double[] coordinates = googleMapService.extractLocationFromLink(dto.getLocationUrl());

        branch.setName(dto.getName());
        branch.setLocationUrl(dto.getLocationUrl());
        branch.setLatitude(coordinates[0]);
        branch.setLongitude(coordinates[1]);
        branch.setCampaignRadiusMeters(dto.getCampaignRadiusMeters());
        branch.setOpeningTime(dto.getOpeningTime());
        branch.setClosingTime(dto.getClosingTime());

        branchRepository.save(branch);

        return mapToDTOOUT(branch);
    }

    public void deleteBranch(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        if (salesRecordRepository.existsByBranchId(branchId)) {
            throw new ApiException("Cannot delete branch because it has sales records");
        }

        if (campaignRepository.existsByBranchId(branchId)) {
            throw new ApiException("Cannot delete branch because it has campaigns");
        }

        if (!monthlyReportRepository.findMonthlyReportsByBranchId(branchId).isEmpty()) {
            throw new ApiException("Cannot delete branch because it has monthly reports");
        }

        branchRepository.delete(branch);
    }

    public BranchOut activateBranch(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        if (branch.getStore().getStatus() != StoreStatus.ACTIVE) {
            throw new ApiException("Cannot activate branch because its store is not active");
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

    public BranchRadiusOut getRecommendedRadius(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        List<Customer> customers = customerRepository.findCustomersByLocationConsentTrue();

        int customersWithin500 = countCustomersInsideRadius(branch, customers, 500);
        int customersWithin1500 = countCustomersInsideRadius(branch, customers, 1500);
        int customersWithin3000 = countCustomersInsideRadius(branch, customers, 3000);
        int customersWithin5000 = countCustomersInsideRadius(branch, customers, 5000);
        int customersWithin7000 = countCustomersInsideRadius(branch, customers, 7000);
        int customersWithin10000 = countCustomersInsideRadius(branch, customers, 10000);
        int customersWithin20000 = countCustomersInsideRadius(branch, customers, 20000);
        int customersWithin40000 = countCustomersInsideRadius(branch, customers, 40000);

        Integer recommendedRadius;
        String reason;

        if (customersWithin500 >= 20) {
            recommendedRadius = 500;
            reason = "High customer density near the branch. A smaller radius is recommended.";
        } else if (customersWithin1500 >= 20) {
            recommendedRadius = 1500;
            reason = "Good customer density within 1.5 km. This radius balances reach and relevance.";
        } else if (customersWithin3000 >= 20) {
            recommendedRadius = 3000;
            reason = "Moderate nearby customer density. A 3 km radius is recommended.";
        } else if (customersWithin5000 >= 20) {
            recommendedRadius = 5000;
            reason = "Customer density is lower nearby. A wider 5 km radius is recommended.";
        } else if (customersWithin7000 >= 15) {
            recommendedRadius = 7000;
            reason = "Few customers are close to the branch. A 7 km radius is recommended to improve reach.";
        } else if (customersWithin10000 >= 10) {
            recommendedRadius = 10000;
            reason = "Customer density is low. A 10 km radius is recommended.";
        } else if (customersWithin20000 >= 5) {
            recommendedRadius = 20000;
            reason = "The branch appears to be in a low-density area. A wider 20 km radius is recommended.";
        } else {
            recommendedRadius = 40000;
            reason = "Very few nearby customers. The maximum 40 km radius is recommended, but contact strategy should be reviewed.";
        }

        int customersInsideRecommendedRadius =
                countCustomersInsideRadius(branch, customers, recommendedRadius);

        branch.setRecommendedRadiusMeters(recommendedRadius);
        branchRepository.save(branch);

        return new BranchRadiusOut(
                branch.getId(),
                branch.getName(),
                branch.getCampaignRadiusMeters(),
                recommendedRadius,
                customersInsideRecommendedRadius,
                reason
        );
    }

    public BranchOut applyRecommendedRadius(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        if (branch.getRecommendedRadiusMeters() == null) {
            getRecommendedRadius(branchId);
            branch = branchRepository.findBranchById(branchId);
        }

        branch.setCampaignRadiusMeters(branch.getRecommendedRadiusMeters());

        branchRepository.save(branch);

        return mapToDTOOUT(branch);
    }

    private void validateBranchLimit(Store store) {
        int currentBranches = branchRepository.findBranchesByStoreId(store.getId()).size();

        Subscription subscription = getCurrentSubscriptionForLimit(store.getStoreOwner().getId());

        int maxBranchesPerStore = 3;

        if (subscription != null) {
            maxBranchesPerStore = subscription.getPlanType().getMaxBranchesPerStore();
        }

        if (currentBranches >= maxBranchesPerStore) {
            throw new ApiException("Branch limit reached for this store. Please contact us to add more branches.");
        }
    }

    private Subscription getCurrentSubscriptionForLimit(Integer storeOwnerId) {
        Subscription activeSubscription =
                subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        storeOwnerId,
                        SubscriptionStatus.ACTIVE
                );

        if (activeSubscription != null && !activeSubscription.getEndDate().isBefore(java.time.LocalDate.now())) {
            return activeSubscription;
        }

        return subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                storeOwnerId,
                SubscriptionStatus.PENDING
        );
    }

    private int countCustomersInsideRadius(Branch branch, List<Customer> customers, Integer radiusMeters) {
        int count = 0;

        for (Customer customer : customers) {
            double distance = calculateDistanceInMeters(
                    branch.getLatitude(),
                    branch.getLongitude(),
                    customer.getLatitude(),
                    customer.getLongitude()
            );

            if (distance <= radiusMeters) {
                count++;
            }
        }

        return count;
    }

    private double calculateDistanceInMeters(Double lat1, Double lon1, Double lat2, Double lon2) {
        final int earthRadiusMeters = 6371000;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusMeters * c;
    }

    private BranchOut mapToDTOOUT(Branch branch) {
        return new BranchOut(
                branch.getId(),
                branch.getName(),
                branch.getLocationUrl(),
                branch.getLatitude(),
                branch.getLongitude(),
                branch.getStatus(),
                branch.getCampaignRadiusMeters(),
                branch.getRecommendedRadiusMeters(),
                branch.getOpeningTime(),
                branch.getClosingTime(),
                branch.getStore().getId(),
                branch.getStore().getName()
        );
    }
}