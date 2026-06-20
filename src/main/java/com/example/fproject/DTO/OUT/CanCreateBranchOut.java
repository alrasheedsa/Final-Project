package com.example.fproject.DTO.OUT;
 
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CanCreateBranchOut {
 
    private Boolean canCreate;
    private Integer maxBranches;
    private Integer currentBranches;
    private Integer remainingBranches;
    private String  message;
}
 