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
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreStatus status;

    @Column(nullable = false)
    private Integer campaignRadiusMeters;

    @OneToOne
    @JoinColumn(name = "store_owner_id", unique = true)
    private StoreOwner storeOwner;

    @OneToMany(mappedBy = "store")
    @JsonIgnore
    private Set<SalesRecord> salesRecords;

    @OneToMany(mappedBy = "store")
    @JsonIgnore
    private Set<Campaign> campaigns;

    @OneToMany(mappedBy = "store")
    @JsonIgnore
    private Set<MonthlyReport> monthlyReports;
}