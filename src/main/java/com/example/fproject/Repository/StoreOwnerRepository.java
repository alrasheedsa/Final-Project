package com.example.fproject.Repository;

import com.example.fproject.Model.StoreOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreOwnerRepository extends JpaRepository<StoreOwner, Integer> {

    StoreOwner findStoreOwnerById(Integer id);

    StoreOwner findStoreOwnerByUserId(Integer userId);

}