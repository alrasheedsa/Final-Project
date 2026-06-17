package com.example.fproject.DTO.IN;

import com.example.fproject.Enum.MessageStatus;
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
public class CampaignMessageRequestIn {

    @NotEmpty(message = "Message text is required")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s؟?.,!:_\\-()%/]+$", message = "Message text contains invalid characters")
    private String messageText;

    @NotNull(message = "Distance is required")
    @PositiveOrZero(message = "Distance cannot be negative")
    private Double distanceKm;

    @NotNull(message = "Duration is required")
    @PositiveOrZero(message = "Duration cannot be negative")
    private Integer durationMinutes;

    @NotEmpty(message = "Distance text is required")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s./:_\\-]+$", message = "Distance text contains invalid characters")
    private String distanceText;

    @NotNull(message = "Message status is required")
    private MessageStatus status;

    @NotNull(message = "Sent at is required")
    private LocalDateTime sentAt;

    @NotNull(message = "Campaign id is required")
    private Integer campaignId;

    @NotNull(message = "Customer id is required")
    private Integer customerId;

    private Integer customerAnswerId;
}
