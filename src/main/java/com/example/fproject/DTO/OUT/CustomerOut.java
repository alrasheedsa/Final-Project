package com.example.fproject.DTO.OUT;

import com.example.fproject.Enum.RoleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerOut {

    // User fields
    private Integer id;

    private String fullName;

    private String phone;

    private String email;

    private Boolean enabled;

    private LocalDateTime createdAt;

    // Customer fields
    private String locationUrl;

    private Double latitude;

    private Double longitude;

    private Boolean locationConsent;
}
