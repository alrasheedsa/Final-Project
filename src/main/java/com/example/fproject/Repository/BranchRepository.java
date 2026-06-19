package com.example.fproject.Repository;

import com.example.fproject.Model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Integer> {

    Branch findBranchById(Integer id);

    Boolean existsBranchByNameAndStoreId(String name, Integer storeId);

    List<Branch> findBranchesByStoreId(Integer storeId);

    Integer countBranchesByStoreId(Integer storeId);

    boolean existsByStoreId(Integer storeId);
}