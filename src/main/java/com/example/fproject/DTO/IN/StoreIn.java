package com.example.fproject.DTO.IN;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoreIn {

    @NotBlank(message = "Store name is required")
    @Size(min = 2, max = 100, message = "Store name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Business type is required")
    private String businessType;

    @NotBlank(message = "Commercial register number is required")
    @Size(min = 10, max = 10, message = "Commercial register number must be 10 digits")
    @Pattern(regexp = "^\\d{10}$", message = "Commercial register number must contain digits only")
    private String commercialRegisterNo;
}