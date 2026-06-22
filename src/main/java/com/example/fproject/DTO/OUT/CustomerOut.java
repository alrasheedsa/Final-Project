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

    private Integer id;

    private String fullName;

    private String phone;

    private String email;

    private LocalDateTime createdAt;

    private String locationUrl;

    private Double latitude;

    private Double longitude;

}
