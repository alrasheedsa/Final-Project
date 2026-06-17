package com.example.fproject.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "campaign_results")
public class CampaignResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer sentCount;

    @Column(nullable = false)
    private Integer redeemedCount;

    @Column(nullable = false)
    private Double conversionRate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String aiSummary;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "campaignResult")
    @JsonIgnore
    private Campaign campaign;

    @ManyToOne
    @JoinColumn(name = "monthly_report_id")
    private MonthlyReport monthlyReport;
}
