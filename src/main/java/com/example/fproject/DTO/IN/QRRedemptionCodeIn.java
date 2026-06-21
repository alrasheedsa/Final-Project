package com.example.fproject.DTO.IN;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QRRedemptionCodeIn {

    @NotEmpty(message = "QR code is required")
    @Pattern(regexp = "^[A-Za-z0-9._\\-]+$", message = "QR code contains invalid characters")
    private String code;

    @NotEmpty(message = "Customer phone is required")
    @Pattern(regexp = "^\\+966\\d{9}$", message = "Customer phone must be a valid Saudi number starting with +966")
    private String customerPhone;
}
