package com.example.fproject.DTO.OUT;

import com.example.fproject.Enum.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentOut {

    private Integer id;

    private Double amount;

    private String transactionId;

    private String paymentProvider;

    private PaymentStatus status;

    private LocalDateTime paidAt;

    private Integer subscriptionId;
}