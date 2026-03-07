package com.fierceadventurer.smartportfoliobackend.content.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectRequest(
        @NotBlank(message = "Title is Required")
        String title,
        @NotBlank(message = "Description is Required")
        String description,

        String techStack,
        String githubUrl,
        String liveUrl
) {
}
