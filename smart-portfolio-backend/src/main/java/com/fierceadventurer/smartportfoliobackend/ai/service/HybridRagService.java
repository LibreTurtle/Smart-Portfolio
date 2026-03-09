package com.fierceadventurer.smartportfoliobackend.ai.service;

import com.fierceadventurer.smartportfoliobackend.ai.dto.ChatRequest;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

public interface HybridRagService {
    Flux<String> streamQuestion(ChatRequest request);
}
