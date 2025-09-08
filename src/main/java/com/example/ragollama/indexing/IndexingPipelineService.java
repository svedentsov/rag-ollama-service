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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис, реализующий унифицированный и идемпотентный конвейер индексации.
 * <p>
 * Этот сервис является ядром процесса индексации. Перед добавлением новых
 * чанков он удаляет все старые чанки для того же документа, обеспечивая
 * консистентность и актуальность данных.
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
    private final VectorStoreRepository vectorStoreRepository; // НОВАЯ ЗАВИСИМОСТЬ

    /**
     * Выполняет полный, унифицированный и идемпотентный конвейер индексации.
     *
     * @param request DTO с данными для индексации.
     */
    @Transactional // Оборачиваем всю операцию в транзакцию для атомарности
    public void process(IndexingRequest request) {
        log.info("Запуск идемпотентного конвейера индексации для источника: '{}', ID: {}",
                request.sourceName(), request.documentId());
        // ШАГ 0: Удаляем все существующие чанки для этого документа
        int deletedCount = vectorStoreRepository.deleteByDocumentId(request.documentId());
        if (deletedCount > 0) {
            log.info("Удалено {} старых чанков для документа '{}' перед обновлением.", deletedCount, request.sourceName());
        }
        // ШАГ 1: Маскирование чувствительных данных.
        String redactedText = piiRedactionService.redact(request.textContent());
        // ШАГ 2: Очистка текста от HTML и прочего "шума".
        String cleanedText = dataCleaningService.cleanDocumentText(redactedText);
        // ШАГ 3: Подготовка метаданных.
        Map<String, Object> metadata = new HashMap<>();
        Optional.ofNullable(request.metadata()).ifPresent(metadata::putAll);
        metadata.put("source", request.sourceName());
        metadata.put("documentId", request.documentId());
        Document documentToSplit = new Document("passage: " + cleanedText, metadata);
        // ШАГ 4: Разбиение на чанки.
        List<Document> chunks = textSplitterService.split(documentToSplit);
        log.debug("Создано {} чанков для документа '{}'", chunks.size(), request.sourceName());
        // ШАГ 5: Индексация новых чанков и очистка кэша.
        if (!chunks.isEmpty()) {
            vectorStore.add(chunks);
            vectorCacheService.evictAll(); // Важно очищать кэш после ЛЮБОГО изменения в Vector Store
            log.info("Документ '{}' (ID: {}) успешно (пере)индексирован, добавлено {} чанков.",
                    request.sourceName(), request.documentId(), chunks.size());
        } else {
            log.warn("Для документа '{}' (ID: {}) не было создано ни одного чанка. Все старые версии удалены.",
                    request.sourceName(), request.documentId());
        }
    }
}
