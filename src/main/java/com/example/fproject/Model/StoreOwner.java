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
@Table(name = "store_owners")
public class StoreOwner {

    @Id
    private Integer id;

    @Column(nullable = false, unique = true)
    private String commercialRegisterNo;

    @Column(nullable = false)
    private Boolean commercialRegisterVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreStatus accountStatus;

    @Column(nullable = false)
    private String businessType;

    @Column(nullable = false)
    private Integer campaignRadiusMeters;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @OneToOne(mappedBy = "storeOwner")
    @JsonIgnore
    private Store store;

    @OneToMany(mappedBy = "storeOwner")
    @JsonIgnore
    private Set<Subscription> subscriptions;
}