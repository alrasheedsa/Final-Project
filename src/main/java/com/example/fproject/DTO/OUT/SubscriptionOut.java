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
public class SubscriptionOut {

    private Integer id;

    private SubscriptionPlanType planType;

    private LocalDate startDate;

    private LocalDate endDate;

    private SubscriptionStatus status;

    private String storeOwnerName;
}