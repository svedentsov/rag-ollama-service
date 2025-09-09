package com.example.ragollama.rag.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChainOfThoughtResponse(
        List<ExtractedFact> extractedFacts,
        String finalAnswer
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExtractedFact(String fact, String source) {
    }
}
