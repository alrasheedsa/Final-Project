package com.example.fproject.DTO.IN;

import com.example.fproject.Enum.CampaignType;
import com.example.fproject.Enum.SuggestionStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignSuggestionIn {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Offer text is required")
    private String offerText;

    private CampaignType campaignType;

    @NotNull(message = "Suggested start time is required")
    private LocalTime suggestedStartTime;

    @NotNull(message = "Suggested end time is required")
    private LocalTime suggestedEndTime;

    @NotNull(message = "Target customers count is required")
    @PositiveOrZero(message = "Target customers count must be zero or greater")
    private Integer targetCustomersCount;

    @NotNull(message = "Discount value is required")
    @PositiveOrZero(message = "Discount value must be zero or greater")
    @DecimalMax(value = "100.0", message = "Discount value cannot be more than 100")
    private Double discountValue;

    @NotBlank(message = "Suggested product name is required")
    private String suggestedProductName;

    @NotNull(message = "Suggestion round is required")
    @Positive(message = "Suggestion round must be greater than zero")
    private Integer suggestionRound;

    @NotNull(message = "AI analysis id is required")
    private Integer aiAnalysisId;

    @AssertTrue(message = "Suggested end time must be after suggested start time")
    public boolean isSuggestedEndTimeAfterStartTime() {
        if (suggestedStartTime == null || suggestedEndTime == null) {
            return true;
        }
        return suggestedEndTime.isAfter(suggestedStartTime);
    }
}
