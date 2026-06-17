package com.example.fproject.DTO.IN;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiQuestionRequestIn {

    @NotEmpty(message = "Question is required")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s؟?.,!:_\\-()]+$", message = "Question contains invalid characters")
    private String questionText;

    @NotEmpty(message = "Option A is required")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s؟?.,!:_\\-()]+$", message = "Option A contains invalid characters")
    private String optionA;

    @NotEmpty(message = "Option B is required")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s؟?.,!:_\\-()]+$", message = "Option B contains invalid characters")
    private String optionB;

    @NotEmpty(message = "Option C is required")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s؟?.,!:_\\-()]+$", message = "Option C contains invalid characters")
    private String optionC;

    @NotEmpty(message = "Correct option is required")
    @Pattern(regexp = "^[ABC]$", message = "Correct option must be A, B, or C")
    private String correctOption;

    private Integer campaignId;
}
