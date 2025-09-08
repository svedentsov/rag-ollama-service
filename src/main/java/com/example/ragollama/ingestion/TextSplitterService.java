package com.example.ragollama.ingestion;

import com.example.ragollama.ingestion.splitter.DocumentSplitterStrategy;
import com.example.ragollama.ingestion.splitter.SplitterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис-оркестратор для интеллектуального разбиения текста на чанки.
 * Реализует стратегию "Small-to-Big" для улучшения качества контекста.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TextSplitterService {

    private final IngestionProperties ingestionProperties;
    private final List<DocumentSplitterStrategy> strategies;

    private static final List<String> DEFAULT_DELIMITERS = List.of("\n\n", "\n", "(?<=[.!?])\\s+");

    public List<Document> split(Document document) {
        log.info("Применение стратегии Small-to-Big для документа: {}", document.getMetadata().get("source"));

        // 1. Создаем родительские чанки
        SplitterConfig parentConfig = new SplitterConfig(
                ingestionProperties.chunking().defaultChunkSize() * 4,
                ingestionProperties.chunking().chunkOverlap() * 2,
                DEFAULT_DELIMITERS
        );
        List<Document> parentChunks = getStrategyFor(document).split(document, parentConfig);

        List<Document> finalChildChunks = new ArrayList<>();
        String originalDocumentId = (String) document.getMetadata().get("documentId");
        AtomicInteger chunkCounter = new AtomicInteger(0);

        // 2. Для каждого родительского чанка создаем дочерние
        for (Document parentChunk : parentChunks) {
            SplitterConfig childConfig = new SplitterConfig(
                    ingestionProperties.chunking().defaultChunkSize(),
                    ingestionProperties.chunking().chunkOverlap(),
                    DEFAULT_DELIMITERS
            );
            List<Document> childChunks = getStrategyFor(parentChunk).split(parentChunk, childConfig);

            // 3. Обогащаем дочерние чанки метаданными
            for (Document childChunk : childChunks) {
                Map<String, Object> newMetadata = new java.util.HashMap<>(childChunk.getMetadata());

                // Наш кастомный, трассируемый ID для цитирования
                String customChunkId = String.format("%s:%d", originalDocumentId, chunkCounter.getAndIncrement());
                newMetadata.put("chunkId", customChunkId);

                // Убеждаемся, что documentId исходного документа сохранен в метаданных
                newMetadata.put("documentId", originalDocumentId);

                // FIX: Генерируем новый, валидный UUID для первичного ключа таблицы vector_store
                String primaryKey = UUID.randomUUID().toString();

                finalChildChunks.add(new Document(primaryKey, childChunk.getText(), newMetadata));
            }
        }

        log.info("Создано {} дочерних чанков для документа '{}'", finalChildChunks.size(), document.getMetadata().get("source"));
        return finalChildChunks;
    }

    private DocumentSplitterStrategy getStrategyFor(Document document) {
        return strategies.stream()
                .filter(s -> s.supports(document))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Не найдена подходящая стратегия для документа."));
    }
}
