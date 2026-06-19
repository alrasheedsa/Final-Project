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
import jakarta.transaction.Transactional;
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

    @Transactional
    public BranchOut addBranch(Integer storeId, BranchIn dto) {

        Store store = findStoreOrThrow(storeId);

        if (store.getStatus() == StoreStatus.INACTIVE) {
            throw new ApiException("Cannot add a branch to an inactive store");
        }

        if (!Boolean.TRUE.equals(store.getCommercialRegisterVerified())) {
            throw new ApiException("Cannot add a branch before the store's commercial register is verified");
        }

        Subscription subscription = getActiveOrPendingSubscription(store.getStoreOwner().getId());

        long currentCount = countActiveOrPendingBranches(storeId);
        int maxBranches   = subscription.getPlanType().getMaxBranchesPerStore();

        if (currentCount >= maxBranches) {
            throw new ApiException(
                    "Your subscription plan allows only " + maxBranches + " branch(es) per store. "
                            + "Deactivate an existing branch or upgrade your plan."
            );
        }

        if (branchRepository.existsBranchByNameAndStoreId(dto.getName(), storeId)) {
            throw new ApiException("A branch with this name already exists in this store");
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

        branch.setStatus(resolveBranchStatus(subscription, store));

        branchRepository.save(branch);

        return mapToOut(branch);
    }

    public List<BranchOut> getAllBranches() {
        return branchRepository.findAll()
                .stream()
                .map(this::mapToOut)
                .toList();
    }

    public BranchOut getBranchById(Integer branchId) {
        return mapToOut(findBranchOrThrow(branchId));
    }

    public List<BranchOut> getBranchesByStoreId(Integer storeId) {
        findStoreOrThrow(storeId);
        return branchRepository.findBranchesByStoreId(storeId)
                .stream()
                .map(this::mapToOut)
                .toList();
    }

    @Transactional
    public BranchOut updateBranch(Integer branchId, BranchIn dto) {

        Branch branch = findBranchOrThrow(branchId);

        if (!branch.getName().equals(dto.getName())
                && branchRepository.existsBranchByNameAndStoreId(dto.getName(), branch.getStore().getId())) {
            throw new ApiException("A branch with this name already exists in this store");
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

        return mapToOut(branch);
    }

    @Transactional
    public BranchOut activateBranch(Integer branchId) {

        Branch branch = findBranchOrThrow(branchId);

        if (branch.getStatus() == StoreStatus.ACTIVE) {
            throw new ApiException("Branch is already active");
        }

        if (branch.getStore().getStatus() != StoreStatus.ACTIVE) {
            throw new ApiException("Cannot activate branch: its store is not active");
        }

        Subscription activeSubscription = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        branch.getStore().getStoreOwner().getId(),
                        SubscriptionStatus.ACTIVE
                );

        if (activeSubscription == null || activeSubscription.getEndDate().isBefore(LocalDate.now())) {
            throw new ApiException("Cannot activate branch: no active subscription found");
        }

        branch.setStatus(StoreStatus.ACTIVE);
        branchRepository.save(branch);

        return mapToOut(branch);
    }

    @Transactional
    public BranchOut deactivateBranch(Integer branchId) {

        Branch branch = findBranchOrThrow(branchId);

        if (branch.getStatus() == StoreStatus.INACTIVE) {
            throw new ApiException("Branch is already inactive");
        }

        branch.setStatus(StoreStatus.INACTIVE);
        branchRepository.save(branch);

        return mapToOut(branch);
    }

    @Transactional
    public void deleteBranch(Integer branchId) {

        Branch branch = findBranchOrThrow(branchId);

        if (branch.getStatus() == StoreStatus.ACTIVE) {
            throw new ApiException("Cannot delete an active branch. Deactivate it first");
        }

        if (salesRecordRepository.existsByBranchId(branchId)) {
            throw new ApiException("Cannot delete branch because it has sales records");
        }

        if (campaignRepository.existsByBranchId(branchId)) {
            throw new ApiException("Cannot delete branch because it has campaigns");
        }

        if (monthlyReportRepository.existsMonthlyReportByBranchIdAndMonthAndYear(branchId, 0, 0)
                || !monthlyReportRepository.findMonthlyReportsByBranchId(branchId).isEmpty()) {
            throw new ApiException("Cannot delete branch because it has monthly reports");
        }

        branchRepository.delete(branch);
    }


    public boolean isBranchSubscribed(Integer branchId) {

        Branch branch = findBranchOrThrow(branchId);

        Store store = branch.getStore();
        if (store == null || store.getStoreOwner() == null) {
            return false;
        }

        if (branch.getStatus() != StoreStatus.ACTIVE) {
            return false;
        }

        if (store.getStatus() != StoreStatus.ACTIVE) {
            return false;
        }

        Subscription activeSubscription = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        store.getStoreOwner().getId(),
                        SubscriptionStatus.ACTIVE
                );

        return activeSubscription != null
                && !activeSubscription.getEndDate().isBefore(LocalDate.now());
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
            throw new ApiException("Working hours must be in HH:mm format (e.g. 09:00, 22:30)");
        }
    }

    public BranchRadiusOut getRecommendedRadius(Integer branchId) {

        Branch branch = findBranchOrThrow(branchId);

        if (branch.getLatitude() == null || branch.getLongitude() == null) {
            throw new ApiException("Branch location coordinates are missing");
        }

        List<Customer> customers = customerRepository.findCustomersByLocationConsentTrue();

        int within500   = countCustomersInsideRadius(branch, customers, 500);
        int within1500  = countCustomersInsideRadius(branch, customers, 1500);
        int within3000  = countCustomersInsideRadius(branch, customers, 3000);
        int within5000  = countCustomersInsideRadius(branch, customers, 5000);
        int within7000  = countCustomersInsideRadius(branch, customers, 7000);
        int within10000 = countCustomersInsideRadius(branch, customers, 10000);
        int within20000 = countCustomersInsideRadius(branch, customers, 20000);
        int within40000 = countCustomersInsideRadius(branch, customers, 40000);

        OpenAiService.BranchRadiusAIResult aiResult = openAiService.recommendBranchRadius(
                branch.getName(),
                branch.getLatitude(),
                branch.getLongitude(),
                branch.getCampaignRadiusMeters(),
                within500, within1500, within3000, within5000,
                within7000, within10000, within20000, within40000
        );

        Integer recommendedRadius = aiResult.recommendedRadiusMeters();
        int customersInside = countCustomersInsideRadius(branch, customers, recommendedRadius);

        branch.setRecommendedRadiusMeters(recommendedRadius);
        branchRepository.save(branch);

        return new BranchRadiusOut(
                branch.getId(),
                branch.getName(),
                branch.getCampaignRadiusMeters(),
                recommendedRadius,
                customersInside,
                aiResult.reason()
        );
    }

    @Transactional
    public BranchOut applyRecommendedRadius(Integer branchId) {

        Branch branch = findBranchOrThrow(branchId);

        if (branch.getRecommendedRadiusMeters() == null) {
            getRecommendedRadius(branchId);
            branch = findBranchOrThrow(branchId);
        }

        branch.setCampaignRadiusMeters(branch.getRecommendedRadiusMeters());
        branchRepository.save(branch);

        return mapToOut(branch);
    }

    private Branch findBranchOrThrow(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);
        if (branch == null) {
            throw new ApiException("Branch not found");
        }
        return branch;
    }

    private Store findStoreOrThrow(Integer storeId) {
        Store store = storeRepository.findStoreById(storeId);
        if (store == null) {
            throw new ApiException("Store not found");
        }
        return store;
    }

    private Subscription getActiveOrPendingSubscription(Integer storeOwnerId) {

        Subscription active = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        storeOwnerId, SubscriptionStatus.ACTIVE
                );

        if (active != null && !active.getEndDate().isBefore(LocalDate.now())) {
            return active;
        }

        Subscription pending = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        storeOwnerId, SubscriptionStatus.PENDING
                );

        if (pending != null) {
            return pending;
        }

        throw new ApiException("Store owner must choose a subscription plan before adding branches");
    }

    private long countActiveOrPendingBranches(Integer storeId) {
        return branchRepository.findBranchesByStoreId(storeId)
                .stream()
                .filter(b -> b.getStatus() == StoreStatus.ACTIVE
                        || b.getStatus() == StoreStatus.PENDING)
                .count();
    }

    private StoreStatus resolveBranchStatus(Subscription subscription, Store store) {
        return subscription.getStatus() == SubscriptionStatus.ACTIVE
                && store.getStatus() == StoreStatus.ACTIVE
                ? StoreStatus.ACTIVE
                : StoreStatus.PENDING;
    }


    private int countCustomersInsideRadius(Branch branch, List<Customer> customers, Integer radiusMeters) {
        int count = 0;
        for (Customer customer : customers) {
            if (customer.getLatitude() == null || customer.getLongitude() == null) continue;
            double distance = calculateDistanceInMeters(
                    branch.getLatitude(), branch.getLongitude(),
                    customer.getLatitude(), customer.getLongitude()
            );
            if (distance <= radiusMeters) count++;
        }
        return count;
    }

    private double calculateDistanceInMeters(Double lat1, Double lon1, Double lat2, Double lon2) {

        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.MAX_VALUE;
        }

        final int R = 6_371_000;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private BranchOut mapToOut(Branch branch) {
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
