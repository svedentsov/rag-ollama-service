package com.example.ragollama.ingestion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Типобезопасная конфигурация для процесса индексации (ingestion).
 */
@Validated
@ConfigurationProperties(prefix = "app.ingestion")
public record IngestionProperties(Chunking chunking) {

    /**
     * Компактный конструктор для установки значений по умолчанию.
     * Вызывается Spring Boot при создании бина. Если `chunking` из .yml
     * приходит как null, мы создаем объект `Chunking` с дефолтными значениями.
     */
    public IngestionProperties {
        if (chunking == null) {
            chunking = new Chunking(512, 64);
        }
    }

    /**
     * @param defaultChunkSize Целевой размер одного чанка в токенах.
     * @param chunkOverlap     Размер пересечения между чанками в токенах.
     */
    public record Chunking(
            @Min(64) @Max(2048) int defaultChunkSize,
            @Min(8) @Max(512) int chunkOverlap
    ) {
    }
}
