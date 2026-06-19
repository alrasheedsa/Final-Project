package com.example.fproject.DTO.IN;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubscriptionIn {

    @NotBlank(message = "Plan type is required")
    private String planType;
}