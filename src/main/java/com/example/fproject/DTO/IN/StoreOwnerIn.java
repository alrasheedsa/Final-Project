package com.example.fproject.DTO.IN;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoreOwnerIn {

    // User fields
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^05\\d{8}$", message = "Phone must be a valid Saudi number starting with 05")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    // StoreOwner fields
    @NotBlank(message = "Commercial register number is required")
    @Size(min = 10, max = 10, message = "Commercial register number must be 10 digits")
    @Pattern(regexp = "^\\d{10}$", message = "Commercial register number must contain digits only")
    private String commercialRegisterNo;

    @NotBlank(message = "Business type is required")
    private String businessType;

    @NotNull(message = "Campaign radius is required")
    @Min(value = 500, message = "Campaign radius must be at least 500 meters")
    private Integer campaignRadiusMeters;

    // Store fields
    @NotBlank(message = "Store name is required")
    @Size(min = 2, max = 100, message = "Store name must be between 2 and 100 characters")
    private String storeName;

    @NotNull(message = "Store latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90")
    @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90")
    private Double latitude;

    @NotNull(message = "Store longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180")
    @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180")
    private Double longitude;
}