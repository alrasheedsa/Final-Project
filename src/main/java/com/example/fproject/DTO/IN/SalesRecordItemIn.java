package com.example.fproject.DTO.IN;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalesRecordItemIn {

    @NotBlank(message = "Product name is required")
    private String productName;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @PositiveOrZero(message = "Unit price must be zero or greater")
    private Double unitPrice;

    @NotNull(message = "Sale date is required")
    @PastOrPresent(message = "Sale date cannot be in the future")
    private LocalDate saleDate;

    @NotNull(message = "Sale time is required")
    private LocalTime saleTime;
}
