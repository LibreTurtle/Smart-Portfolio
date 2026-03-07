package com.fierceadventurer.smartportfoliobackend.ai.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Slf4j
@Repository
@RequiredArgsConstructor
public class SemanticCacheRepositoryImpl implements SemanticCacheRepository{

    private final JdbcClient jdbcClient;

    private static final double DISTANCE_THRESHOLD = 0.05;
    @Override
    public Optional<String> findCacheResponse(List<Double> embedding) {
        String vectorString = embedding.toString();

        String sql = """
                SELECT cached_response
                FROM ai_semantic_cache
                WHERE prompt_embedding <=> ?::vector < ?
                ORDER BY prompt_embedding <=> ?::vector ASC
                LIMIT 1
                """;
        return jdbcClient.sql(sql)
                .param(vectorString)
                .param(DISTANCE_THRESHOLD)
                .param(vectorString)
                .query(String.class)
                .optional();
    }

    @Override
    public void saveToCache(String prompt, List<Double> embedding, String response) {
        String vectorString = embedding.toString();

        String sql = """
                INSERT INTO ai_semantic_cache (prompt_text , prompt_embedding, cached_response)
                VALUES (?, ?::vector, ?)
                """;

        jdbcClient.sql(sql)
                .param(prompt)
                .param(vectorString)
                .param(response)
                .update();

        log.info("Saved new AI response to semantic cache for prompt: {}", prompt);
    }
}
