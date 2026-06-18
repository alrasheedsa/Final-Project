package com.example.fproject.Repository;

import com.example.fproject.Model.CampaignMessage;
import com.example.fproject.Enum.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignMessageRepository extends JpaRepository<CampaignMessage, Integer> {

    Boolean existsByCampaignIdAndCustomerId(Integer campaignId, Integer customerId);

    List<CampaignMessage> findAllByCampaignId(Integer campaignId);

    List<CampaignMessage> findAllByCustomerId(Integer customerId);

    List<CampaignMessage> findAllByCustomerIdAndStatusOrderBySentAtDesc(Integer customerId, MessageStatus status);
}
