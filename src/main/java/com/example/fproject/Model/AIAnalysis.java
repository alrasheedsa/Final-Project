package com.example.fproject.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_analyses")
public class AIAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

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

    @Column(columnDefinition = "TEXT")
    private String seasonalPatterns;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recommendation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String aiSummary;

    @Column(nullable = false)
    private LocalDateTime analyzedAt;

    @OneToOne
    @JoinColumn(name = "sales_record_id", nullable = false, unique = true)
    @JsonIgnore
    private SalesRecord salesRecord;

    @OneToMany(mappedBy = "aiAnalysis", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<CampaignSuggestion> campaignSuggestions;
}
