package com.example.fproject.Model;

import com.example.fproject.Enum.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Double amount;

    @Column(unique = true)
    private String transactionId;

    @Column(unique = true)
    private String checkoutId;

    @Column(length = 1000)
    private String checkoutUrl;

    @Column(nullable = false)
    private String paymentProvider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private LocalDateTime paidAt;

    @ManyToOne
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;
}