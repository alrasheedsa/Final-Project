package com.example.fproject.DTO.OUT;
 
import com.example.fproject.Enum.CampaignStatus;
import com.example.fproject.Enum.StoreStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BranchDashboardOut {
 
    private Integer branchId;
    private String branchName;
    private StoreStatus status;
    private String openingTime;
    private String closingTime;
    private Integer currentRadiusMeters;
    private Integer recommendedRadiusMeters;
    private Integer customersInRadius;
    private Integer totalCampaigns;
    private Integer activeCampaigns;
    private Integer lastCampaignId;
    private String lastCampaignTitle;
    private CampaignStatus lastCampaignStatus;
}
 