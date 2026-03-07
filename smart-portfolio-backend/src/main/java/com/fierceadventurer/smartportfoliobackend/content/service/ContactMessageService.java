package com.fierceadventurer.smartportfoliobackend.content.service;

import com.fierceadventurer.smartportfoliobackend.content.dto.ContactMessageRequest;
import com.fierceadventurer.smartportfoliobackend.content.dto.ContactMessageResponse;

public interface ContactMessageService {
    ContactMessageResponse submitMessage(ContactMessageRequest request);
}
