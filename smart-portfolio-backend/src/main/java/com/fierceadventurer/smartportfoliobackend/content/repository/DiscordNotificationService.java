package com.fierceadventurer.smartportfoliobackend.content.repository;

import com.fierceadventurer.smartportfoliobackend.content.dto.ContactMessageRequest;

public interface DiscordNotificationService {
    void sendContactNotification(ContactMessageRequest request);
}
