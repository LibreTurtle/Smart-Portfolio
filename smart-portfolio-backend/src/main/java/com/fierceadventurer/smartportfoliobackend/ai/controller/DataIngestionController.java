package com.fierceadventurer.smartportfoliobackend.ai.controller;

import com.fierceadventurer.smartportfoliobackend.ai.dto.IngestionRequest;
import com.fierceadventurer.smartportfoliobackend.ai.service.DataIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ingest")
public class DataIngestionController {
    private final DataIngestionService ingestionService;

    @PostMapping
    public ResponseEntity<String> ingestPortfolioData(@Valid @RequestBody IngestionRequest request){
        ingestionService.ingestData(request.content());
        return ResponseEntity.ok("Data successfully chunked, embedded, and saved to pgvector!");
    }
}

