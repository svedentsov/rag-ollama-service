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
 * Сервис, реализующий унифицированный и идемпотентный конвейер для индексации
 * и управления жизненным циклом документов в векторном хранилище.
 * Является единой точкой входа для всех операций, изменяющих векторный индекс.
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
     * <p>
     * Сначала удаляет все существующие чанки для данного `documentId`, а затем
     * выполняет очистку, разделение и индексацию нового контента. Это гарантирует
     * консистентность данных и предотвращает появление дубликатов или "осиротевших" чанков.
     *
     * @param request DTO с данными для индексации.
     * @return {@link Mono<Void>}, завершающийся после выполнения индексации.
     */
    @Transactional
    public Mono<Void> process(IndexingRequest request) {
        log.info("Запуск идемпотентного конвейера индексации для источника: '{}', ID: {}",
                request.sourceName(), request.documentId());

        // Шаг 1: Идемпотентное удаление старых версий
        return vectorStoreRepository.deleteByDocumentId(request.documentId())
                .flatMap(deletedCount -> {
                    if (deletedCount > 0) {
                        log.info("Удалено {} старых чанков для документа '{}' перед обновлением.", deletedCount, request.sourceName());
                    }

                    // Шаг 2: Подготовка текста и метаданных
                    String redactedText = piiRedactionService.redact(request.textContent());
                    String cleanedText = dataCleaningService.cleanDocumentText(redactedText);

                    Map<String, Object> metadata = new HashMap<>();
                    Optional.ofNullable(request.metadata()).ifPresent(metadata::putAll);
                    metadata.put("source", request.sourceName());
                    metadata.put("documentId", request.documentId());
                    metadata.put("embedding_model_version", this.embeddingModelVersion);

                    // Шаг 3: Разделение на чанки
                    Document documentToSplit = new Document("passage: " + cleanedText, metadata);
                    List<Document> chunks = textSplitterService.split(documentToSplit);
                    log.debug("Создано {} чанков для документа '{}'", chunks.size(), request.sourceName());

                    // Шаг 4: Добавление новых чанков в VectorStore и инвалидация кэша
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

    /**
     * Асинхронно и идемпотентно удаляет все чанки, связанные с документом, из векторного хранилища.
     * После успешного удаления инвалидирует кэш результатов поиска.
     *
     * @param request Запрос, содержащий `documentId` для удаления.
     * @return {@link Mono<Void>}, завершающийся после удаления.
     */
    @Transactional
    public Mono<Void> delete(IndexingRequest request) {
        log.info("Запуск удаления чанков для документа ID: {}", request.documentId());
        return vectorStoreRepository.deleteByDocumentId(request.documentId())
                .doOnSuccess(deletedCount -> {
                    if (deletedCount > 0) {
                        vectorCacheService.evictAll();
                        log.info("Удалено {} чанков для документа '{}'. Кэш поиска очищен.", deletedCount, request.documentId());
                    } else {
                        log.warn("Не найдено чанков для удаления по документу ID: {}", request.documentId());
                    }
                })
                .then();
    }
}
