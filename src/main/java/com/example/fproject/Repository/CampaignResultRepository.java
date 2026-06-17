package com.example.fproject.Repository;

import com.example.fproject.Model.CampaignResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignResultRepository extends JpaRepository<CampaignResult, Integer> {
}
