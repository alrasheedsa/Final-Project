package com.example.fproject.DTO.OUT;

import com.example.fproject.Enum.SuggestionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignSuggestionOut {

    private Integer id;

    private String title;

    private String description;

    private String offerText;

    private String campaignType;

    private LocalTime suggestedStartTime;

    private LocalTime suggestedEndTime;

    private Integer targetCustomersCount;

    private Double discountValue;

    private String suggestedProductName;

    private SuggestionStatus approvalStatus;

    private Integer suggestionRound;

    private Integer aiAnalysisId;

    private Integer campaignId;
}
