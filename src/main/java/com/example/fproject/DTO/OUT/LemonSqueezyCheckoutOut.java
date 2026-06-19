package com.example.fproject.DTO.OUT;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LemonSqueezyCheckoutOut {

    private Integer localPaymentId;

    private Integer subscriptionId;

    private Double amount;

    private Long amountInCents;

    private String currency;

    private String description;

    private String checkoutId;

    private String checkoutUrl;

    private String message;
}
