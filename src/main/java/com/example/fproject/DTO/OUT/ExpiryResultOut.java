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
public class ExpiryResultOut {

    private Integer expiredCount;
    private Integer deactivatedStores;
    private Integer deactivatedBranches;
    private LocalDateTime processedAt;
}
