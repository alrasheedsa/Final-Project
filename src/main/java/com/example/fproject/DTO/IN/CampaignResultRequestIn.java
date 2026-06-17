package com.example.fproject.DTO.IN;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class CampaignResultRequestIn {

    @NotNull(message = "Sent count is required")
    @PositiveOrZero(message = "Sent count cannot be negative")
    private Integer sentCount;

    @NotNull(message = "Redeemed count is required")
    @PositiveOrZero(message = "Redeemed count cannot be negative")
    private Integer redeemedCount;

    @NotNull(message = "Conversion rate is required")
    @PositiveOrZero(message = "Conversion rate cannot be negative")
    private Double conversionRate;

    @NotEmpty(message = "AI summary is required")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s؟?.,!:_\\-()%]+$", message = "AI summary contains invalid characters")
    private String aiSummary;

    @NotNull(message = "Created at is required")
    private LocalDateTime createdAt;

    private Integer campaignId;
    private Integer monthlyReportId;
}
