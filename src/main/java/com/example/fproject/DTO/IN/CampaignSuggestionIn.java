package com.example.fproject.DTO.IN;

import com.example.fproject.Enum.CampaignType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
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

    @NotNull(message = "Campaign type is required")
    private CampaignType campaignType;

    @NotNull(message = "Suggested start time is required")
    private LocalTime suggestedStartTime;

    @NotNull(message = "Suggested end time is required")
    private LocalTime suggestedEndTime;

    @NotNull(message = "Suggested start date is required")
    private LocalDate suggestedStartDate;

    @NotNull(message = "Suggested end date is required")
    private LocalDate suggestedEndDate;

    @NotNull(message = "Target customers count is required")
    @PositiveOrZero(message = "Target customers count must be zero or greater")
    private Integer targetCustomersCount;

    @NotNull(message = "Discount value is required")
    @PositiveOrZero(message = "Discount value must be zero or greater")
    @DecimalMax(value = "100.0", message = "Discount value cannot be more than 100")
    private Double discountValue;

    @NotBlank(message = "Suggested product name is required")
    private String suggestedProductName;

    @AssertTrue(message = "Suggested end time must be after suggested start time")
    public boolean isSuggestedEndTimeAfterStartTime() {
        if (suggestedStartTime == null || suggestedEndTime == null) {
            return true;
        }
        return suggestedEndTime.isAfter(suggestedStartTime);
    }

    @AssertTrue(message = "Suggested end date must not be before suggested start date")
    public boolean isSuggestedEndDateValid() {
        if (suggestedStartDate == null || suggestedEndDate == null) {
            return true;
        }

        return !suggestedEndDate.isBefore(suggestedStartDate);
    }
}
