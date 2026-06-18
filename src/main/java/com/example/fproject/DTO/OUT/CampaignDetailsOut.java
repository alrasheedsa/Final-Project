package com.example.fproject.DTO.OUT;

import com.example.fproject.Enum.CampaignStatus;
import com.example.fproject.Enum.CampaignType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignDetailsOut {

    private Integer id;

    private String title;

    private String description;

    private String offerText;

    private CampaignType campaignType;

    private CampaignStatus status;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private Integer targetCustomersCount;

    private Integer sentCount;

    private Integer redeemedCount;

    private Integer remainingCoupons;

    private Double usageRate;

    private Integer branchId;

    private String branchName;

    private String storeName;

    private Integer campaignSuggestionId;

    private Integer aiQuestionId;

    private Integer qrCodeId;
}
