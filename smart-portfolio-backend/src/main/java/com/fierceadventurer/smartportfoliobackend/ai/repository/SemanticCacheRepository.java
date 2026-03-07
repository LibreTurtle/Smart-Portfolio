package com.fierceadventurer.smartportfoliobackend.ai.repository;

import java.util.List;
import java.util.Optional;

public interface SemanticCacheRepository {
    Optional<String> findCacheResponse(List<Double> embedding);

    void saveToCache(String prompt, List<Double> embedding , String response);
}
