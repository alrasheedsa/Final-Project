package com.example.fproject.DTO.IN;

import com.example.fproject.Enum.SubscriptionPlanType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionIn {

    @NotNull(message = "Plan type is required")
    private SubscriptionPlanType planType;
}