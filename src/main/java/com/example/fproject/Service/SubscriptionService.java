package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.OUT.CanCreateBranchOut;
import com.example.fproject.DTO.OUT.ExpiryResultOut;
import com.example.fproject.DTO.OUT.StoreOwnerDashboardOut;
import com.example.fproject.DTO.OUT.SubscriptionLimitsOut;
import com.example.fproject.DTO.OUT.SubscriptionPlanOut;
import com.example.fproject.DTO.OUT.SubscriptionStatusOut;
import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Enum.SubscriptionPlanType;
import com.example.fproject.Enum.SubscriptionStatus;
import com.example.fproject.Model.*;
import com.example.fproject.Repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final LemonSqueezyService lemonSqueezyService;
    private final StoreRepository storeRepository;
    private final BranchRepository branchRepository;
    private final CampaignRepository campaignRepository;

    private static final int RENEWAL_WINDOW_DAYS = 7;

    public List<SubscriptionPlanOut> getSubscriptionPlans() {
        return Arrays.stream(SubscriptionPlanType.values())
                .map(plan -> new SubscriptionPlanOut(
                        plan, plan.getPrice(), plan.getMaxStores(),
                        plan.getMaxBranchesPerStore(), plan.getDurationMonths()
                )).toList();
    }

    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    public Subscription getSubscriptionById(Integer subscriptionId) {
        return findSubscriptionOrThrow(subscriptionId);
    }

    public List<Subscription> getSubscriptionsByStoreOwner(Integer userId) {
        StoreOwner owner = findStoreOwnerByUserIdOrThrow(userId);
        return subscriptionRepository.findSubscriptionsByStoreOwnerId(owner.getId());
    }

    public Subscription getActiveSubscription(Integer userId) {
        StoreOwner owner = findStoreOwnerByUserIdOrThrow(userId);
        Subscription subscription = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(owner.getId(), SubscriptionStatus.ACTIVE);
        if (subscription == null) throw new ApiException("No active subscription found");
        return subscription;
    }

    public SubscriptionStatusOut getSubscriptionStatus(Integer userId) {
        StoreOwner owner = findStoreOwnerByUserIdOrThrow(userId);

        Subscription subscription = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(owner.getId(), SubscriptionStatus.ACTIVE);

        if (subscription == null) {
            subscription = subscriptionRepository
                    .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(owner.getId(), SubscriptionStatus.PENDING);
        }

        if (subscription == null) throw new ApiException("No subscription found for this store owner");
        return buildSubscriptionStatusOut(subscription);
    }

    public StoreOwnerDashboardOut getStoreOwnerDashboard(Integer userId) {
        StoreOwner storeOwner = findStoreOwnerByUserIdOrThrow(userId);

        SubscriptionStatusOut subscriptionStatus = null;
        try { subscriptionStatus = getSubscriptionStatus(userId); } catch (ApiException ignored) {}

        List<Store> stores = storeRepository.findStoresByStoreOwnerId(storeOwner.getId());
        long activeStores  = stores.stream().filter(s -> s.getStatus() == StoreStatus.ACTIVE).count();
        long pendingStores = stores.stream().filter(s -> s.getStatus() == StoreStatus.PENDING).count();

        long totalBranches  = 0;
        long activeBranches = 0;
        for (Store store : stores) {
            List<Branch> branches = branchRepository.findBranchesByStoreId(store.getId());
            totalBranches  += branches.size();
            activeBranches += branches.stream().filter(b -> b.getStatus() == StoreStatus.ACTIVE).count();
        }

        Campaign lastCampaign = null;
        for (Store store : stores) {
            for (Branch branch : branchRepository.findBranchesByStoreId(store.getId())) {
                for (Campaign c : campaignRepository.findAllByBranchId(branch.getId())) {
                    if (lastCampaign == null || c.getStartDateTime().isAfter(lastCampaign.getStartDateTime()))
                        lastCampaign = c;
                }
            }
        }

        SubscriptionLimitsOut limits = buildLimits(storeOwner.getId(), stores);

        return new StoreOwnerDashboardOut(
                storeOwner.getId(),
                storeOwner.getUser().getFullName(),
                storeOwner.getUser().getEmail(),
                subscriptionStatus,
                stores.size(), (int) activeStores, (int) pendingStores,
                (int) totalBranches, (int) activeBranches,
                lastCampaign != null ? lastCampaign.getId() : null,
                lastCampaign != null ? lastCampaign.getTitle() : null,
                lastCampaign != null ? lastCampaign.getStatus() : null,
                limits
        );
    }

    public SubscriptionLimitsOut getSubscriptionLimits(Integer userId) {
        StoreOwner owner = findStoreOwnerByUserIdOrThrow(userId);
        List<Store> stores = storeRepository.findStoresByStoreOwnerId(owner.getId());
        return buildLimits(owner.getId(), stores);
    }

    public CanCreateBranchOut canCreateBranch(Integer userId, Integer storeId) {
        StoreOwner owner = findStoreOwnerByUserIdOrThrow(userId);

        Store store = storeRepository.findStoreById(storeId);
        if (store == null) throw new ApiException("Store not found");

        if (!store.getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("Store does not belong to this store owner");

        Subscription subscription = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(owner.getId(), SubscriptionStatus.ACTIVE);

        if (subscription == null) {
            subscription = subscriptionRepository
                    .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(owner.getId(), SubscriptionStatus.PENDING);
        }

        if (subscription == null) return new CanCreateBranchOut(false, 0, 0, 0, "No subscription found");

        int maxBranches = subscription.getPlanType().getMaxBranchesPerStore();
        long currentBranches = branchRepository.findBranchesByStoreId(storeId)
                .stream()
                .filter(b -> b.getStatus() == StoreStatus.ACTIVE || b.getStatus() == StoreStatus.PENDING)
                .count();

        boolean canCreate = currentBranches < maxBranches;
        int remaining     = (int) Math.max(maxBranches - currentBranches, 0);
        String message = canCreate
                ? "You can add " + remaining + " more branch(es) to this store"
                : "You have reached the maximum number of branches for your plan";

        return new CanCreateBranchOut(canCreate, maxBranches, (int) currentBranches, remaining, message);
    }

    @Transactional
    public String renewSubscription(Integer userId, String newPlanTypeText) {
        StoreOwner owner = findStoreOwnerByUserIdOrThrow(userId);

        Subscription currentSubscription = findCurrentSubscriptionForRenewal(owner.getId());

        SubscriptionStatus currentStatus = currentSubscription.getStatus();
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), currentSubscription.getEndDate());

        boolean isExpiredOrCancelled = currentStatus == SubscriptionStatus.EXPIRED
                || currentStatus == SubscriptionStatus.CANCELLED;
        boolean isInRenewalWindow = currentStatus == SubscriptionStatus.ACTIVE
                && daysRemaining <= RENEWAL_WINDOW_DAYS;

        if (!isExpiredOrCancelled && !isInRenewalWindow) {
            throw new ApiException(
                    "Renewal is not available yet. You can renew when " + RENEWAL_WINDOW_DAYS
                            + " days or less remain. Days remaining: " + Math.max(daysRemaining, 0)
            );
        }

        if (currentStatus == SubscriptionStatus.ACTIVE
                && currentSubscription.getLemonSubscriptionId() != null
                && !currentSubscription.getLemonSubscriptionId().isBlank()) {
            lemonSqueezyService.cancelLemonSubscription(currentSubscription.getLemonSubscriptionId());
            currentSubscription.setStatus(SubscriptionStatus.CANCELLED);
            currentSubscription.setLemonStatus("cancelled");
            subscriptionRepository.save(currentSubscription);
        }

        return lemonSqueezyService.createSubscriptionCheckout(newPlanTypeText, owner.getId());
    }

    @Transactional
    public void cancelSubscription(Integer userId, Integer subscriptionId) {
        StoreOwner owner = findStoreOwnerByUserIdOrThrow(userId);
        Subscription subscription = findSubscriptionOrThrow(subscriptionId);

        if (!subscription.getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("You do not have permission to cancel this subscription");

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED)
            throw new ApiException("Subscription is already cancelled");

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE
                && subscription.getLemonSubscriptionId() != null
                && !subscription.getLemonSubscriptionId().isBlank()) {
            lemonSqueezyService.cancelLemonSubscription(subscription.getLemonSubscriptionId());
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setLemonStatus("cancelled");
        subscriptionRepository.save(subscription);
        deactivateStoresAndBranches(subscription);
    }

    @Transactional
    public String checkExpiredSubscriptions() {
        List<Subscription> expired = subscriptionRepository
                .findSubscriptionsByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, LocalDate.now());

        for (Subscription subscription : expired) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            deactivateStoresAndBranches(subscription);
        }

        return "Checked " + expired.size() + " expired subscription(s) and updated successfully";
    }
    // ── helpers ──────────────────────────────────────────────────────────────

    private StoreOwner findStoreOwnerByUserIdOrThrow(Integer userId) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null) throw new ApiException("Store owner not found");
        return owner;
    }

    private SubscriptionLimitsOut buildLimits(Integer storeOwnerId, List<Store> stores) {
        Subscription subscription = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(storeOwnerId, SubscriptionStatus.ACTIVE);
        if (subscription == null) {
            subscription = subscriptionRepository
                    .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(storeOwnerId, SubscriptionStatus.PENDING);
        }
        if (subscription == null) return new SubscriptionLimitsOut(null, 0, 0, 0, 0, 0, 0, false, false);

        int maxStores   = subscription.getPlanType().getMaxStores();
        int maxBranches = subscription.getPlanType().getMaxBranchesPerStore();

        long usedStores = stores.stream()
                .filter(s -> s.getStatus() == StoreStatus.ACTIVE || s.getStatus() == StoreStatus.PENDING).count();
        long usedBranches = stores.stream()
                .flatMap(s -> branchRepository.findBranchesByStoreId(s.getId()).stream())
                .filter(b -> b.getStatus() == StoreStatus.ACTIVE || b.getStatus() == StoreStatus.PENDING).count();

        return new SubscriptionLimitsOut(
                subscription.getPlanType().name(),
                maxStores, (int) usedStores, maxStores - (int) usedStores,
                maxBranches, (int) usedBranches, maxBranches - (int) usedBranches,
                usedStores < maxStores, true
        );
    }

    private SubscriptionStatusOut buildSubscriptionStatusOut(Subscription subscription) {
        LocalDate today    = LocalDate.now();
        LocalDate endDate  = subscription.getEndDate();
        long daysRemaining = ChronoUnit.DAYS.between(today, endDate);
        boolean isExpired  = endDate.isBefore(today);
        SubscriptionStatus status = subscription.getStatus();

        boolean canRenew = status == SubscriptionStatus.EXPIRED
                || status == SubscriptionStatus.CANCELLED
                || isExpired
                || (status == SubscriptionStatus.ACTIVE && daysRemaining <= RENEWAL_WINDOW_DAYS);

        return new SubscriptionStatusOut(
                subscription.getId(), subscription.getPlanType(), status,
                subscription.getStartDate(), endDate,
                Math.max(daysRemaining, 0), isExpired, canRenew,
                buildStatusMessage(status, daysRemaining, isExpired)
        );
    }

    private String buildStatusMessage(SubscriptionStatus status, long daysRemaining, boolean isExpired) {
        if (status == SubscriptionStatus.CANCELLED) return "Your subscription has been cancelled. You can subscribe to a new plan.";
        if (status == SubscriptionStatus.EXPIRED || isExpired) return "Your subscription has expired. Please renew to continue using the service.";
        if (status == SubscriptionStatus.PENDING) return "Your subscription is pending payment. Complete your payment to activate it.";
        if (daysRemaining <= 0) return "Your subscription has expired today. Please renew now.";
        if (daysRemaining == 1) return "Your subscription expires tomorrow. Renew now to avoid interruption.";
        if (daysRemaining <= RENEWAL_WINDOW_DAYS) return "Your subscription expires in " + daysRemaining + " days. You can renew now.";
        return "Your subscription is active. " + daysRemaining + " days remaining.";
    }

    private Subscription findCurrentSubscriptionForRenewal(Integer storeOwnerId) {
        Subscription active = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(storeOwnerId, SubscriptionStatus.ACTIVE);
        if (active != null) return active;

        Subscription expired = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(storeOwnerId, SubscriptionStatus.EXPIRED);
        if (expired != null) return expired;

        Subscription cancelled = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(storeOwnerId, SubscriptionStatus.CANCELLED);
        if (cancelled != null) return cancelled;

        throw new ApiException("No subscription found to renew");
    }

    private int[] deactivateStoresAndBranches(Subscription subscription) {
        int deactivatedStores = 0;
        int deactivatedBranches = 0;
        List<Store> stores = storeRepository.findStoresByStoreOwnerId(subscription.getStoreOwner().getId());
        for (Store store : stores) {
            if (store.getStatus() == StoreStatus.ACTIVE) {
                store.setStatus(StoreStatus.INACTIVE);
                storeRepository.save(store);
                deactivatedStores++;
                for (Branch branch : branchRepository.findBranchesByStoreId(store.getId())) {
                    if (branch.getStatus() == StoreStatus.ACTIVE) {
                        branch.setStatus(StoreStatus.INACTIVE);
                        branchRepository.save(branch);
                        deactivatedBranches++;
                    }
                }
            }
        }
        return new int[]{deactivatedStores, deactivatedBranches};
    }

    private Subscription findSubscriptionOrThrow(Integer subscriptionId) {
        Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);
        if (subscription == null) throw new ApiException("Subscription not found");
        return subscription;
    }
}