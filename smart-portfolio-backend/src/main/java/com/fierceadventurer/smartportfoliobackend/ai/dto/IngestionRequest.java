package com.fierceadventurer.smartportfoliobackend.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record IngestionRequest(
        @NotBlank(message = "Content cannot be empty")
        String Content
) {
}
