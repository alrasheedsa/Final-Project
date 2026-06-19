package com.example.fproject.DTO.OUT;

import com.example.fproject.Enum.QRRedemptionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QRRedemptionResponseOut {

    private Integer id;
    private LocalDateTime redeemedAt;
    private QRRedemptionStatus status;
    private Integer qrCodeId;
    private Integer campaignId;
    private Integer customerId;
}
