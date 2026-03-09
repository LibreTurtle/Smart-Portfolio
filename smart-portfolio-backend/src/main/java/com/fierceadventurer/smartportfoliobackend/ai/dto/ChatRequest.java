package com.fierceadventurer.smartportfoliobackend.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "Question cannot be empty")
        String question
){}
