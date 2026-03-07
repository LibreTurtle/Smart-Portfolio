package com.fierceadventurer.smartportfoliobackend.content.dto;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    String title,
    String description,
    String techStack,
    String githubUrl,
    String liveUrl
){};

