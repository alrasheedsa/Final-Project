package com.example.fproject.DTO.IN;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AIAnalysisIn {

    @NotBlank(message = "Top products is required")
    private String topProducts;

    @NotBlank(message = "Low products is required")
    private String lowProducts;

    @NotBlank(message = "Peak hours is required")
    private String peakHours;

    @NotBlank(message = "Slow hours is required")
    private String slowHours;

    @NotBlank(message = "Surplus products is required")
    private String surplusProducts;

    private String seasonalPatterns;

    @NotBlank(message = "Recommendation is required")
    private String recommendation;

    @NotBlank(message = "AI summary is required")
    private String aiSummary;
}
