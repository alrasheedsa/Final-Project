package com.example.fproject.DTO.OUT;

import com.example.fproject.Enum.SubscriptionPlanType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionPlanOut {

    private SubscriptionPlanType planType;
    private Double price;
    private Integer maxStores;
    private Integer maxBranchesPerStore;
    private Integer durationMonths;
}
