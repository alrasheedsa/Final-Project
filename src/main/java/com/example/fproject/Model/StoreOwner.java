package com.example.fproject.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
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
@Table(name = "store_owners")
public class StoreOwner {

    @Id
    private Integer id;

    @Column(nullable = false, unique = true)
    private String commercialRegisterNo;

    @Column(nullable = false)
    private Boolean verified = false;

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
