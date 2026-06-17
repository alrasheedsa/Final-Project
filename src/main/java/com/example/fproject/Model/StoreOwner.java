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
@Table(name = "store_owners")
public class StoreOwner {

    @Id
    private Integer id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @OneToMany(mappedBy = "storeOwner")
    @JsonIgnore
    private Set<Store> stores;

    @OneToMany(mappedBy = "storeOwner")
    @JsonIgnore
    private Set<Subscription> subscriptions;
}