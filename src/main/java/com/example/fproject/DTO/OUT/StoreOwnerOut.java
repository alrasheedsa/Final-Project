package com.example.fproject.DTO.OUT;

import com.example.fproject.Enum.RoleType;
import com.example.fproject.Enum.StoreStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoreOwnerOut {

    // User fields
    private Integer id;

    private String fullName;

    private String phone;

    private String email;

    private Boolean enabled;

    private LocalDateTime createdAt;

    // StoreOwner fields
    private String commercialRegisterNo;

    private Boolean commercialRegisterVerified;

    private StoreStatus accountStatus;

    private String businessType;

    private Integer campaignRadiusMeters;

    // Store fields
    private StoreOut store;
}