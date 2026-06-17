package com.example.fproject.DTO.IN;

import com.example.fproject.Enum.QRRedemptionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QRRedemptionRequestIn {

    @NotNull(message = "Redeemed at is required")
    private LocalDateTime redeemedAt;

    @NotNull(message = "QR redemption status is required")
    private QRRedemptionStatus status;

    @NotNull(message = "QR code id is required")
    private Integer qrCodeId;

    @NotNull(message = "Campaign id is required")
    private Integer campaignId;
}
