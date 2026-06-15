package com.example.fproject.Model;

import com.example.fproject.Enum.CampaignStatus;
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
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer maxCustomers;

    @Column(nullable = false)
    private Integer usedCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    @OneToOne
    @JoinColumn(name = "campaign_suggestion_id", unique = true)
    private CampaignSuggestion campaignSuggestion;

    @OneToOne
    @JoinColumn(name = "ai_question_id", unique = true)
    private AIQuestion aiQuestion;

    @OneToMany(mappedBy = "campaign")
    @JsonIgnore
    private Set<CampaignMessage> campaignMessages;

    @OneToMany(mappedBy = "campaign")
    @JsonIgnore
    private Set<QRCode> qrCodes;

    @OneToOne
    @JoinColumn(name = "campaign_result_id", unique = true)
    private CampaignResult campaignResult;
}
