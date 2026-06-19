package com.example.fproject.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "monthly_reports")
public class MonthlyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Double totalSales;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String topProducts;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String lowProducts;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String peakHours;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String slowHours;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String surplusProducts;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String aiSummary;

    @Column(nullable = false)
    private String pdfUrl;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

}