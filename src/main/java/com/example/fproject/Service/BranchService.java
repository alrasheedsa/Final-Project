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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
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
    private final OpenAiService openAiService;

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

        Subscription subscription = getActiveOrPendingSubscription(store.getStoreOwner().getId());

        Integer currentBranchesCount = branchRepository.countBranchesByStoreId(storeId);
        Integer maxBranchesPerStore = subscription.getPlanType().getMaxBranchesPerStore();

        if (currentBranchesCount >= maxBranchesPerStore) {
            throw new ApiException("Your current subscription plan allows only "
                    + maxBranchesPerStore + " branch(es) per store");
        }

        if (branchRepository.existsBranchByNameAndStoreId(dto.getName(), storeId)) {
            throw new ApiException("Branch name already exists for this store");
        }

        validateWorkingHours(dto.getOpeningTime(), dto.getClosingTime());

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
        branch.setStore(store);

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE
                && store.getStatus() == StoreStatus.ACTIVE) {
            branch.setStatus(StoreStatus.ACTIVE);
        } else {
            branch.setStatus(StoreStatus.PENDING);
        }

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

        validateWorkingHours(dto.getOpeningTime(), dto.getClosingTime());

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

        Subscription activeSubscription =
                subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        branch.getStore().getStoreOwner().getId(),
                        SubscriptionStatus.ACTIVE
                );

        if (activeSubscription == null || activeSubscription.getEndDate().isBefore(LocalDate.now())) {
            throw new ApiException("Cannot activate branch without an active subscription");
        }


        branch.setStatus(StoreStatus.ACTIVE);
        branchRepository.save(branch);

        return mapToDTOOUT(branch);
    }

    public boolean isBranchSubscribed(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        Store store = branch.getStore();
        if (store == null || store.getStoreOwner() == null) {
            return false;
        }

        Subscription activeSubscription =
                subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        store.getStoreOwner().getId(),
                        SubscriptionStatus.ACTIVE
                );

        return activeSubscription != null
                && !activeSubscription.getEndDate().isBefore(LocalDate.now())
                && branch.getStatus() == StoreStatus.ACTIVE
                && store.getStatus() == StoreStatus.ACTIVE;
    }

    public void validateWorkingHours(String openingTime, String closingTime) {
        if (openingTime == null || openingTime.isBlank()) {
            throw new ApiException("Opening time is required");
        }

        if (closingTime == null || closingTime.isBlank()) {
            throw new ApiException("Closing time is required");
        }

        try {
            LocalTime opening = LocalTime.parse(openingTime.trim());
            LocalTime closing = LocalTime.parse(closingTime.trim());

            if (!closing.isAfter(opening)) {
                throw new ApiException("Closing time must be after opening time");
            }
        } catch (DateTimeParseException e) {
            throw new ApiException("Working hours must use HH:mm format");
        }
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

        if (branch.getLatitude() == null || branch.getLongitude() == null) {
            throw new ApiException("Branch location is missing");
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

        OpenAiService.BranchRadiusAIResult aiResult =
                openAiService.recommendBranchRadius(
                        branch.getName(),
                        branch.getLatitude(),
                        branch.getLongitude(),
                        branch.getCampaignRadiusMeters(),
                        customersWithin500,
                        customersWithin1500,
                        customersWithin3000,
                        customersWithin5000,
                        customersWithin7000,
                        customersWithin10000,
                        customersWithin20000,
                        customersWithin40000
                );

        Integer recommendedRadius = aiResult.recommendedRadiusMeters();

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
                aiResult.reason()
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

    private Subscription getActiveOrPendingSubscription(Integer storeOwnerId) {

        Subscription activeSubscription =
                subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        storeOwnerId,
                        SubscriptionStatus.ACTIVE
                );

        if (activeSubscription != null && !activeSubscription.getEndDate().isBefore(LocalDate.now())) {
            return activeSubscription;
        }

        Subscription pendingSubscription =
                subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        storeOwnerId,
                        SubscriptionStatus.PENDING
                );

        if (pendingSubscription != null && !pendingSubscription.getEndDate().isBefore(LocalDate.now())) {
            return pendingSubscription;
        }

        throw new ApiException("Store owner must choose a subscription plan before adding branches");
    }

    private int countCustomersInsideRadius(Branch branch, List<Customer> customers, Integer radiusMeters) {

        int count = 0;

        for (Customer customer : customers) {

            if (customer.getLatitude() == null || customer.getLongitude() == null) {
                continue;
            }

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

        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.MAX_VALUE;
        }

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