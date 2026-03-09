package com.fierceadventurer.smartportfoliobackend.ai.service;

import org.springframework.web.multipart.MultipartFile;

public interface DataIngestionService {
    void ingestpdf(MultipartFile file);
}
