package com.example.fproject.DTO.OUT;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResultResponseOut {

    private Integer id;
    private Integer sentCount;
    private Integer redeemedCount;
    private Double conversionRate;
    private String aiSummary;
    private LocalDateTime createdAt;
    private Integer campaignId;
    private Integer monthlyReportId;
}
