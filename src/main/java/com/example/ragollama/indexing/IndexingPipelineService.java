package com.example.ragollama.indexing;

import com.example.ragollama.ingestion.TextSplitterService;
import com.example.ragollama.ingestion.cleaning.DataCleaningService;
import com.example.ragollama.shared.caching.VectorCacheService;
import com.example.ragollama.shared.security.PiiRedactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис, реализующий унифицированный и переиспользуемый конвейер индексации.
 * <p>
 * Этот сервис является ядром процесса индексации и инкапсулирует всю
 * последовательность шагов: маскирование PII, очистка, разбиение на чанки и
 * сохранение в векторное хранилище. Он принимает унифицированный
 * {@link IndexingRequest} и не зависит от источника данных.
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

    /**
     * Выполняет полный, унифицированный конвейер индексации для одного документа.
     *
     * @param request DTO с данными для индексации.
     */
    public void process(IndexingRequest request) {
        log.info("Запуск унифицированного конвейера индексации для источника: '{}', ID: {}",
                request.sourceName(), request.documentId());
        // ШАГ 1: Маскирование чувствительных данных.
        String redactedText = piiRedactionService.redact(request.textContent());
        // ШАГ 2: Очистка текста от HTML и прочего "шума".
        String cleanedText = dataCleaningService.cleanDocumentText(redactedText);

        // ШАГ 3: Подготовка метаданных.
        Map<String, Object> metadata = new HashMap<>();
        Optional.ofNullable(request.metadata()).ifPresent(metadata::putAll);
        metadata.put("source", request.sourceName());
        metadata.put("documentId", request.documentId());

        Document documentToSplit = new Document(cleanedText, metadata);

        // ШАГ 4: Разбиение на чанки с использованием конфигурации по умолчанию.
        List<Document> chunks = textSplitterService.split(documentToSplit);
        log.debug("Создано {} чанков для документа '{}'", chunks.size(), request.sourceName());

        // ШАГ 5: Индексация и очистка кэша.
        if (!chunks.isEmpty()) {
            vectorStore.add(chunks);
            vectorCacheService.evictAll();
            log.info("Документ '{}' (ID: {}) успешно проиндексирован, добавлено {} чанков.",
                    request.sourceName(), request.documentId(), chunks.size());
        } else {
            log.warn("Для документа '{}' (ID: {}) не было создано ни одного чанка.",
                    request.sourceName(), request.documentId());
        }
    }
}
