package com.fierceadventurer.smartportfoliobackend.ai.controller;

import com.fierceadventurer.smartportfoliobackend.ai.service.DataIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ingest")
public class DataIngestionController {
    private final DataIngestionService ingestionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> ingestPdfResume(@RequestParam("file") MultipartFile file){

        if(file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".pdf")){
            return ResponseEntity.badRequest().body("Please upload a valid .pdf file.");
        }

        ingestionService.ingestPdf(file);
        return ResponseEntity.ok("PDF successfully parsed, chunked, embedded, and saved to pgvector!");
    }
}

