package com.example.fproject.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer_answers")
public class CustomerAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String answer;

    @Column(nullable = false)
    private Boolean correct;

    @Column(nullable = false)
    private Integer attempts;

    @OneToOne(mappedBy = "customerAnswer")
    @JsonIgnore
    private CampaignMessage campaignMessage;

    @OneToOne
    @JoinColumn(name = "qr_code_id", unique = true)
    private QRCode qrCode;
}
