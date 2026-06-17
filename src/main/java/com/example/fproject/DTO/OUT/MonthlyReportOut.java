package com.example.fproject.DTO.OUT;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyReportOut {

    private Integer id;

    private Integer month;

    private Integer year;

    private Double totalSales;

    private Integer totalQuantity;

    private String topProducts;

    private String lowProducts;

    private String peakHours;

    private String slowHours;

    private String surplusProducts;

    private String aiSummary;

    private String pdfUrl;

    private LocalDateTime generatedAt;

    private String storeName;
}