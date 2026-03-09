package com.fierceadventurer.smartportfoliobackend.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record IngestionRequest(
        @NotBlank(message = "content cannot be empty")
        String content
) {
}
