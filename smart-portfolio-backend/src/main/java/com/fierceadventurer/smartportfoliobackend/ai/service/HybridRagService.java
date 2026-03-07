package com.fierceadventurer.smartportfoliobackend.ai.service;

import com.fierceadventurer.smartportfoliobackend.ai.dto.ChatRequest;
import org.springframework.ai.chat.model.ChatResponse;

public interface HybridRagService {
    ChatResponse askQuestion(ChatRequest request);
}
