package com.example.fproject.DTO.IN;

import com.example.fproject.Enum.CampaignType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRequestIn {

    @NotEmpty(message = "Campaign title is required")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s؟?.,!:_\\-()]+$", message = "Campaign title contains invalid characters")
    private String title;

    @NotEmpty(message = "Campaign description is required")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s؟?.,!:_\\-()]+$", message = "Campaign description contains invalid characters")
    private String description;

    @NotEmpty(message = "Offer text is required")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s؟?.,!:_\\-()%]+$", message = "Offer text contains invalid characters")
    private String offerText;

    @NotNull(message = "Campaign type is required")
    private CampaignType campaignType;

    @NotNull(message = "Campaign start time is required")
    private LocalDateTime startDateTime;

    @NotNull(message = "Campaign end time is required")
    @Future(message = "Campaign end time must be in the future")
    private LocalDateTime endDateTime;

    @NotNull(message = "Target customers count is required")
    @Positive(message = "Target customers count must be greater than zero")
    private Integer targetCustomersCount;

    @NotNull(message = "Sent count is required")
    @PositiveOrZero(message = "Sent count cannot be negative")
    private Integer sentCount;

    @NotNull(message = "Redeemed count is required")
    @PositiveOrZero(message = "Redeemed count cannot be negative")
    private Integer redeemedCount;

    @NotNull(message = "Branch id is required")
    private Integer branchId;

    private Integer campaignSuggestionId;
    private Integer aiQuestionId;
    private Integer campaignResultId;
}
