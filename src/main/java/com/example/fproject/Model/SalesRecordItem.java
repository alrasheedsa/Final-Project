package com.example.fproject.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sales_record_items")
public class SalesRecordItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double unitPrice;

    @Column(nullable = false)
    private Double totalPrice;

    @Column(nullable = false)
    private LocalDate saleDate;

    @Column(nullable = false)
    private LocalTime saleTime;

    @ManyToOne
    @JoinColumn(name = "sales_record_id", nullable = false)
    private SalesRecord salesRecord;

    @PrePersist
    @PreUpdate
    public void calculateTotalPrice() {
        if (quantity != null && unitPrice != null) {
            totalPrice = quantity * unitPrice;
        }
    }
}
