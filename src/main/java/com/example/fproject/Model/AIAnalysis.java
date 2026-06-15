package com.example.fproject.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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

    @Column(nullable = false)
    private String peakHours;

    @Column(nullable = false)
    private String idleHours;

    @Column(nullable = false)
    private String recommendation;

    @Column(nullable = false)
    private LocalDateTime analyzedAt;

    @OneToOne(mappedBy = "aiAnalysis")
    @JsonIgnore
    private SalesRecord salesRecord;

    @OneToMany(mappedBy = "aiAnalysis")
    @JsonIgnore
    private Set<CampaignSuggestion> campaignSuggestions;
}
