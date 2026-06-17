package com.example.fproject.DTO.OUT;

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
public class SalesRecordItemOut {

    private Integer id;

    private String productName;

    private Integer quantity;

    private Double unitPrice;

    private Double totalPrice;

    private LocalDate saleDate;

    private LocalTime saleTime;

    private Integer salesRecordId;
}
