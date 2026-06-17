package com.example.fproject.Repository;

import com.example.fproject.Model.CampaignMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignMessageRepository extends JpaRepository<CampaignMessage, Integer> {

    Boolean existsByCampaignIdAndCustomerId(Integer campaignId, Integer customerId);
}
