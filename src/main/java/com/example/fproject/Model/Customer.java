package com.example.fproject.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    private Integer id;

    @Column(nullable = false)
    private String locationUrl;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @OneToMany(mappedBy = "customer")
    @JsonIgnore
    private Set<CampaignMessage> campaignMessages;

    @OneToMany(mappedBy = "customer")
    @JsonIgnore
    private Set<CustomerAnswer> customerAnswers;
}
