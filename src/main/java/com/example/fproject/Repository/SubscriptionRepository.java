package com.example.fproject.Repository;

import com.example.fproject.Enum.SubscriptionStatus;
import com.example.fproject.Model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {

    Subscription findSubscriptionById(Integer id);

    List<Subscription> findSubscriptionsByStoreOwnerId(Integer storeOwnerId);

    List<Subscription> findSubscriptionsByStoreOwnerIdAndStatus(Integer storeOwnerId, SubscriptionStatus status);

    Subscription findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(Integer storeOwnerId, SubscriptionStatus status);
}