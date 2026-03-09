package com.fierceadventurer.smartportfoliobackend.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataIngestionServiceImpl implements DataIngestionService{
    private final VectorStore vectorStore;

    @Override
    public void ingestPdf(MultipartFile file) {
        log.info("Starting pdf ingestion process for file: {}", file.getOriginalFilename());
        try{
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(file.getResource());

            List<Document> extractedPages = pdfReader.get();
            log.info("Successfully extracted {} pages from the PDF.", extractedPages.size());

            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> chunkedDocuments = splitter.apply(extractedPages);

            log.info("Split PDF into {} chunks. Generating embeddings and saving to pgvector...", chunkedDocuments.size());
            vectorStore.add(chunkedDocuments);

            log.info("Successfully ingested PDF into the vector store!");

        }catch (Exception e) {
            log.error("Failed to parse and ingest PDF document", e);
            throw new RuntimeException("Failed to process PDF file. Ensure it is a valid text-based PDF.", e);
        }
    }
}
