package com.example.ragollama.indexing;

import com.example.ragollama.ingestion.TextSplitterService;
import com.example.ragollama.ingestion.cleaning.DataCleaningService;
import com.example.ragollama.optimization.VectorStoreRepository;
import com.example.ragollama.shared.caching.VectorCacheService;
import com.example.ragollama.shared.processing.PiiRedactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис, реализующий унифицированный и идемпотентный конвейер индексации.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingPipelineService {

    private final VectorStore vectorStore;
    private final TextSplitterService textSplitterService;
    private final VectorCacheService vectorCacheService;
    private final DataCleaningService dataCleaningService;
    private final PiiRedactionService piiRedactionService;
    private final VectorStoreRepository vectorStoreRepository;

    @Value("${spring.ai.ollama.embedding.options.model-version}")
    private String embeddingModelVersion;

    /**
     * Выполняет полный, транзакционный и идемпотентный процесс индексации документа.
     *
     * @param request DTO с данными для индексации.
     * @return {@link Mono}, завершающийся после выполнения индексации.
     */
    @Transactional
    public Mono<Void> process(IndexingRequest request) {
        log.info("Запуск идемпотентного конвейера индексации для источника: '{}', ID: {}",
                request.sourceName(), request.documentId());

        return vectorStoreRepository.deleteByDocumentId(request.documentId())
                .flatMap(deletedCount -> {
                    if (deletedCount > 0) {
                        log.info("Удалено {} старых чанков для документа '{}' перед обновлением.", deletedCount, request.sourceName());
                    }

                    String redactedText = piiRedactionService.redact(request.textContent());
                    String cleanedText = dataCleaningService.cleanDocumentText(redactedText);

                    Map<String, Object> metadata = new HashMap<>();
                    Optional.ofNullable(request.metadata()).ifPresent(metadata::putAll);
                    metadata.put("source", request.sourceName());
                    metadata.put("documentId", request.documentId());
                    metadata.put("embedding_model_version", this.embeddingModelVersion);

                    Document documentToSplit = new Document("passage: " + cleanedText, metadata);
                    List<Document> chunks = textSplitterService.split(documentToSplit);
                    log.debug("Создано {} чанков для документа '{}'", chunks.size(), request.sourceName());

                    if (!chunks.isEmpty()) {
                        vectorStore.add(chunks);
                        vectorCacheService.evictAll();
                        log.info("Документ '{}' (ID: {}) успешно (пере)индексирован, добавлено {} чанков. Кэш поиска очищен.",
                                request.sourceName(), request.documentId(), chunks.size());
                    } else {
                        log.warn("Для документа '{}' (ID: {}) не было создано ни одного чанка. Все старые версии удалены.",
                                request.sourceName(), request.documentId());
                    }
                    return Mono.empty();
                });
    }
}
