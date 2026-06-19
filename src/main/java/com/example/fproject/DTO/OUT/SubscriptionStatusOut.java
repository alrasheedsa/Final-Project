package com.example.fproject.DTO.OUT;

import com.example.fproject.Enum.SubscriptionPlanType;
import com.example.fproject.Enum.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionStatusOut {

    private Integer subscriptionId;

    private SubscriptionPlanType planType;
    private SubscriptionStatus   status;
    private LocalDate            startDate;
    private LocalDate            endDate;

    private Long daysRemaining;

    private Boolean isExpired;

    private Boolean canRenew;

    private String message;
}