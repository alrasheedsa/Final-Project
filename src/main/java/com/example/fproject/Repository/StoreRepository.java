package com.example.fproject.Repository;

import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, Integer> {

    Store findStoreById(Integer id);

    List<Store> findStoresByStoreOwnerId(Integer storeOwnerId);

    List<Store> findStoresByStatus(StoreStatus status);

    Store findStoreByCommercialRegisterNo(String commercialRegisterNo);

    Boolean existsStoreByCommercialRegisterNo(String commercialRegisterNo);

    Boolean existsStoreByNameAndStoreOwnerId(String name, Integer storeOwnerId);
}