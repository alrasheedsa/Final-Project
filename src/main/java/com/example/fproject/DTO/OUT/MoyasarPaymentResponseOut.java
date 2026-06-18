package com.example.fproject.DTO.OUT;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MoyasarPaymentResponseOut {

    private Integer localPaymentId;

    private Integer subscriptionId;

    private Double amount;

    private String moyasarPaymentId;

    private String moyasarStatus;

    private String transactionUrl;

    private String message;
}