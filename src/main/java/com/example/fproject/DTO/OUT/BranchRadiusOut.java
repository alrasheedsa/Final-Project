package com.example.fproject.DTO.OUT;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BranchRadiusOut {

    private Integer branchId;

    private String branchName;

    private Integer currentRadiusMeters;

    private Integer recommendedRadiusMeters;

    private Integer customersInsideRecommendedRadius;

    private String reason;
}