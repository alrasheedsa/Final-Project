package com.example.fproject.Repository;

import com.example.fproject.Model.CustomerAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerAnswerRepository extends JpaRepository<CustomerAnswer, Integer> {

    Boolean existsByCampaignIdAndCustomerId(Integer campaignId, Integer customerId);

    CustomerAnswer findCustomerAnswerByCampaignMessageId(Integer campaignMessageId);

    List<CustomerAnswer> findAllByCampaignId(Integer campaignId);

    Integer countByCampaignId(Integer campaignId);

    Integer countByCampaignIdAndCorrect(Integer campaignId, Boolean correct);
}
