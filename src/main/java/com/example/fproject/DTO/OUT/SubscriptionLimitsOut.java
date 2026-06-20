
package com.example.fproject.DTO.OUT;
 
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionLimitsOut {
 
    private String  planType;
    private Integer maxStores;
    private Integer usedStores;
    private Integer remainingStores;
    private Integer maxBranchesPerStore;
    private Integer usedBranches;
    private Integer remainingBranches;
    private Boolean canAddStore;
    private Boolean canAddBranch;
}
 