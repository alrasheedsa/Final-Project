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
public class CustomerAnswerResponseOut {

    private Integer id;
    private String selectedOption;
    private Boolean correct;
    private LocalDateTime attemptedAt;
    private Integer customerId;
    private Integer campaignId;
    private Integer campaignMessageId;
}
