package com.fierceadventurer.smartportfoliobackend.content.service;

import com.fierceadventurer.smartportfoliobackend.content.dto.ContactMessageRequest;

public interface DiscordNotificationService {
    void sendContactNotification(ContactMessageRequest request);
}
