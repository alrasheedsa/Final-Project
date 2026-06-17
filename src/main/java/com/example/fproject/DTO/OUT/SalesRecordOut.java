package com.example.fproject.DTO.OUT;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalesRecordOut {

    private Integer id;

    private String fileName;

    private String fileUrl;

    private Integer month;

    private Integer year;

    private LocalDateTime uploadedAt;

    private Integer storeId;

    private Integer itemsCount;

    private Integer aiAnalysisId;
}
