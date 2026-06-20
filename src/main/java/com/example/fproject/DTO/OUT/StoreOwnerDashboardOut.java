package com.example.fproject.DTO.OUT;
 
import com.example.fproject.Enum.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoreOwnerDashboardOut {
 
    private Integer storeOwnerId;
    private String fullName;
    private String email;
    private SubscriptionStatusOut subscriptionStatus;
    private Integer totalStores;
    private Integer activeStores;
    private Integer pendingStores;
    private Integer totalBranches;
    private Integer activeBranches;
    private Integer lastCampaignId;
    private String lastCampaignTitle;
    private CampaignStatus lastCampaignStatus;
    private SubscriptionLimitsOut limits;
}