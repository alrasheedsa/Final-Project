package com.example.fproject.DTO.IN;

import com.example.fproject.Enum.QRCodeStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QRCodeRequestIn {

    @NotEmpty(message = "QR code is required")
    @Pattern(regexp = "^[A-Za-z0-9._\\-]+$", message = "QR code contains invalid characters")
    private String code;

    @NotNull(message = "Max usage count is required")
    @Positive(message = "Max usage count must be greater than zero")
    private Integer maxUsageCount;

    @NotNull(message = "Used count is required")
    @PositiveOrZero(message = "Used count cannot be negative")
    private Integer usedCount;

    @NotNull(message = "QR code status is required")
    private QRCodeStatus status;

    @NotNull(message = "Campaign id is required")
    private Integer campaignId;
}
