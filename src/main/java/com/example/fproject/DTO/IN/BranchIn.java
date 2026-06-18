package com.example.fproject.DTO.IN;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BranchIn {

    @NotBlank(message = "Branch name is required")
    @Size(min = 2, max = 100, message = "Branch name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Location URL is required")
    private String locationUrl;

    @NotNull(message = "Campaign radius is required")
    @Min(value = 500, message = "Campaign radius must be at least 500 meters")
    @Max(value = 40000, message = "Campaign radius must not exceed 40000 meters")
    private Integer campaignRadiusMeters;

    @NotBlank(message = "Opening time is required")
    private String openingTime;

    @NotBlank(message = "Closing time is required")
    private String closingTime;
}