package com.example.fproject.DTO.OUT;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AIAnalysisOut {

    private Integer id;

    private String topProducts;

    private String lowProducts;

    private String peakHours;

    private String slowHours;

    private String surplusProducts;

    private String seasonalPatterns;

    private String recommendation;

    private String aiSummary;

    private LocalDateTime analyzedAt;

    private Integer salesRecordId;

    private Integer campaignSuggestionsCount;
}
