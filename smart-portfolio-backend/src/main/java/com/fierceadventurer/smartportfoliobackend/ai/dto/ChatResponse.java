package com.fierceadventurer.smartportfoliobackend.ai.dto;

public record ChatResponse (
        String answer,
        boolean isCached
){
}
