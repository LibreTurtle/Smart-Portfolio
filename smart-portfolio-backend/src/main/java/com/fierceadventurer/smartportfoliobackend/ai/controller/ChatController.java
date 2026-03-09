package com.fierceadventurer.smartportfoliobackend.ai.controller;

import com.fierceadventurer.smartportfoliobackend.ai.dto.ChatRequest;
import com.fierceadventurer.smartportfoliobackend.ai.service.HybridRagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {
    private final HybridRagService hybridRagService;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askQuestion(@Valid @RequestBody ChatRequest request){
        return hybridRagService.streamQuestion(request);
    }
}
