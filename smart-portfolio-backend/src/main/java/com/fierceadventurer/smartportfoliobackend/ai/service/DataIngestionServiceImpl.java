package com.fierceadventurer.smartportfoliobackend.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataIngestionServiceImpl implements DataIngestionService{
    private final VectorStore vectorStore;

    @Override
    public void ingestData(String content) {
        log.info("Starting data ingestion process...");
        Document rawDocument = new Document(content, Map.of("source", "portfolio-resume"));

        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunkedDocuments = splitter.apply(List.of(rawDocument));

        log.info("Split resume into {} chunks. Generating embeddings and saving to pgvector...", chunkedDocuments.size());

        vectorStore.add(chunkedDocuments);

        log.info("Successfully ingested all data into the vector store!");
    }
}
