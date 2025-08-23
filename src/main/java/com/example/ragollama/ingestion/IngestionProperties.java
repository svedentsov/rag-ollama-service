package com.example.ragollama.ingestion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.ingestion")
public record IngestionProperties(Chunking chunking) {

    public record Chunking(
            @Min(64) @Max(2048) int defaultChunkSize,
            @Min(8) @Max(512) int chunkOverlap
    ) {
    }
}
