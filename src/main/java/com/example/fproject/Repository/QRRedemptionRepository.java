package com.example.fproject.Repository;

import com.example.fproject.Model.QRRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QRRedemptionRepository extends JpaRepository<QRRedemption, Integer> {

    List<QRRedemption> findAllByCampaignId(Integer campaignId);

    Integer countByCampaignId(Integer campaignId);

    Boolean existsByCampaignIdAndCustomerId(Integer campaignId, Integer customerId);
}
