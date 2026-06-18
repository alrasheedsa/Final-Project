package com.example.fproject.DTO.OUT;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MoyasarCheckoutOut {

    private Integer localPaymentId;

    private Integer subscriptionId;

    private Double amount;

    private Long amountInHalalas;

    private String currency;

    private String description;

    private String publishableKey;

    private String callbackUrl;

    private String message;
}