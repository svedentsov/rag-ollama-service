package com.example.ragollama.ingestion;

import com.example.ragollama.ingestion.splitter.DocumentSplitterStrategy;
import com.example.ragollama.ingestion.splitter.SplitterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис-оркестратор для интеллектуального разбиения текста на чанки.
 * Реализует стратегию "Parent Document Retriever" (или Small-to-Big).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TextSplitterService {

    private final IngestionProperties ingestionProperties;
    private final List<DocumentSplitterStrategy> strategies;

    private static final List<String> PARENT_DELIMITERS = List.of("\n\n", "\n");
    private static final List<String> CHILD_DELIMITERS = List.of("(?<=[.!?])\\s+");

    /**
     * Выполняет разделение документа, реализуя стратегию "Parent Document Retriever".
     * <p>
     * Процесс состоит из следующих шагов:
     * <ol>
     *     <li>Исходный документ разбивается на большие "родительские" чанки.</li>
     *     <li>Каждый родительский чанк, в свою очередь, разбивается на более мелкие "дочерние" чанки.</li>
     *     <li>Каждый дочерний чанк обогащается метаданными, содержащими ID и полный текст его родителя.</li>
     *     <li>Только дочерние чанки отправляются на индексацию.</li>
     * </ol>
     *
     * @param document Исходный документ для разделения.
     * @return Список дочерних документов, готовых к индексации.
     */
    public List<Document> split(Document document) {
        log.info("Применение Parent Document стратегии для документа: {}", document.getMetadata().get("source"));
        // 1. Создаем большие родительские чанки
        SplitterConfig parentConfig = new SplitterConfig(
                ingestionProperties.chunking().defaultChunkSize() * 4, // Размер родителя в 4 раза больше
                ingestionProperties.chunking().chunkOverlap() * 2,
                PARENT_DELIMITERS
        );
        List<Document> parentChunks = getStrategyFor(document).split(document, parentConfig);
        List<Document> finalChildChunks = new ArrayList<>();
        String originalDocumentId = (String) document.getMetadata().get("documentId");
        AtomicInteger parentChunkCounter = new AtomicInteger(0);
        AtomicInteger childChunkCounter = new AtomicInteger(0);

        // 2. Для каждого родительского чанка создаем дочерние
        for (Document parentChunk : parentChunks) {
            String parentChunkId = String.format("%s:p%d", originalDocumentId, parentChunkCounter.getAndIncrement());
            SplitterConfig childConfig = new SplitterConfig(
                    ingestionProperties.chunking().defaultChunkSize(),
                    ingestionProperties.chunking().chunkOverlap(),
                    CHILD_DELIMITERS
            );
            List<Document> childChunks = getStrategyFor(parentChunk).split(parentChunk, childConfig);
            // 3. Обогащаем дочерние чанки и добавляем их в финальный список для индексации
            for (Document childChunk : childChunks) {
                Map<String, Object> newMetadata = new HashMap<>(childChunk.getMetadata());
                String childChunkId = String.format("%s:c%d", parentChunkId, childChunkCounter.getAndIncrement());
                newMetadata.put("chunkId", childChunkId);
                newMetadata.put("documentId", originalDocumentId);
                newMetadata.put("parentChunkId", parentChunkId);
                newMetadata.put("parentChunkText", parentChunk.getText());
                String newDocumentId = UUID.randomUUID().toString();
                finalChildChunks.add(new Document(newDocumentId, childChunk.getText(), newMetadata));
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
