package com.fierceadventurer.smartportfoliobackend.content.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ContactMessageRequest(
        @NotBlank(message = "Name is required")
        String senderName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String senderEmail,

        @NotBlank(message = "Message cannot be empty")
        String messageBody
) {}
