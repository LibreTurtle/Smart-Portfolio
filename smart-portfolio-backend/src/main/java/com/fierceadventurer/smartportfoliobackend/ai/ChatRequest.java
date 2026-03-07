package com.fierceadventurer.smartportfoliobackend.ai;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "Question cannot be empty")
        String Question
)  {
}
