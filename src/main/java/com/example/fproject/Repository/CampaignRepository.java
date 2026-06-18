package com.example.fproject.Repository;

import com.example.fproject.Model.Campaign;
import com.example.fproject.Enum.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Integer> {

    Boolean existsByBranchId(Integer branchId);

    List<Campaign> findAllByBranchId(Integer branchId);

    List<Campaign> findAllByBranchIdAndStatus(Integer branchId, CampaignStatus status);
}
