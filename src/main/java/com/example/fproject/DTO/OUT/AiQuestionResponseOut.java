package com.example.fproject.DTO.OUT;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiQuestionResponseOut {

    private Integer id;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String correctOption;
    private Integer campaignId;
}
