package com.example.fproject.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private Integer stockQuantity;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer quantitySold;

    @Column(nullable = false)
    private Double revenue;

    @ManyToOne
    @JoinColumn(name = "sales_record_id")
    private SalesRecord salesRecord;
}
