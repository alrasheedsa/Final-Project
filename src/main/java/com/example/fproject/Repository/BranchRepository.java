package com.example.fproject.Repository;

import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Integer> {

    Branch findBranchById(Integer id);

    List<Branch> findBranchesByStoreId(Integer storeId);

    List<Branch> findBranchesByStatus(StoreStatus status);

    Boolean existsBranchByNameAndStoreId(String name, Integer storeId);

    Boolean existsByStoreId(Integer storeId);
}
