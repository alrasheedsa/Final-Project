package com.example.fproject.Model;

import com.example.fproject.Enum.StoreStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "branches")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String locationUrl;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreStatus status;

    @Column(nullable = false)
    private Integer campaignRadiusMeters;

    @Column
    private Integer recommendedRadiusMeters;

    @Column(nullable = false)
    private String openingTime;

    @Column(nullable = false)
    private String closingTime;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    @OneToMany(mappedBy = "branch")
    @JsonIgnore
    private Set<SalesRecord> salesRecords;

    @OneToMany(mappedBy = "branch")
    @JsonIgnore
    private Set<Campaign> campaigns;

    @OneToMany(mappedBy = "branch")
    @JsonIgnore
    private Set<MonthlyReport> monthlyReports;
}