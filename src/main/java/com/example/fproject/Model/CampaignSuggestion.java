package com.example.fproject.Model;

import com.example.fproject.Enum.SuggestionStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "campaign_suggestions")
public class CampaignSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String offerText;

    @Column(nullable = false)
    private String campaignType;

    @Column(nullable = false)
    private LocalTime suggestedStartTime;

    @Column(nullable = false)
    private LocalTime suggestedEndTime;

    @Column(nullable = false)
    private Integer targetCustomersCount;

    @Column(nullable = false)
    private Double discountValue;

    @Column(nullable = false)
    private String suggestedProductName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionStatus approvalStatus;

    @Column(nullable = false)
    private Integer suggestionRound;

    @ManyToOne
    @JoinColumn(name = "ai_analysis_id", nullable = false)
    private AIAnalysis aiAnalysis;

    @OneToOne(mappedBy = "campaignSuggestion")
    @JsonIgnore
    private Campaign campaign;
}
