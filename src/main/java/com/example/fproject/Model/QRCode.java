package com.example.fproject.Model;

import com.example.fproject.Enum.QRCodeStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "qr_codes")
public class QRCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private Integer maxUsageCount;

    @Column(nullable = false)
    private Integer usedCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QRCodeStatus status;

    @OneToOne
    @JoinColumn(name = "campaign_id", unique = true)
    private Campaign campaign;

    @OneToMany(mappedBy = "qrCode")
    @JsonIgnore
    private Set<QRRedemption> qrRedemptions;
}
