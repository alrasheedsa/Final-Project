package com.example.fproject.DTO.IN;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAnswerRequestIn {

    @NotEmpty(message = "Selected option is required")
    @Pattern(regexp = "^[ABC]$", message = "Selected option must be A, B, or C")
    private String selectedOption;

    @NotNull(message = "Correct status is required")
    private Boolean correct;

    @NotNull(message = "Attempted at is required")
    private LocalDateTime attemptedAt;

    @NotNull(message = "Customer id is required")
    private Integer customerId;

    @NotNull(message = "Campaign id is required")
    private Integer campaignId;

    private Integer campaignMessageId;
}
