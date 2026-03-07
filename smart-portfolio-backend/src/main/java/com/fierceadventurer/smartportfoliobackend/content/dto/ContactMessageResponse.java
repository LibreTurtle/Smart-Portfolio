package com.fierceadventurer.smartportfoliobackend.content.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ContactMessageResponse(
        UUID id,
        String SenderName,
        LocalDateTime submittedAt
) {
}
