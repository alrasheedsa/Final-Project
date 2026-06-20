package com.example.fproject.DTO.OUT;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CampaignRadiusInfoOut {

    private Integer branchId;
    private String branchName;
    private Integer currentRadiusMeters;
    private Integer customersInCurrentRadius;
    private Integer recommendedRadiusMeters;
    private Integer customersInRecommendedRadius;
    private String aiReason;
}
 