package com.example.fproject.Repository;

import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, Integer> {

    Store findStoreById(Integer id);

    Store findStoreByStoreOwnerId(Integer storeOwnerId);

    List<Store> findStoresByStatus(StoreStatus status);

    Boolean existsStoreByStoreOwnerId(Integer storeOwnerId);
}