package com.example.ragollama.service;

import com.example.ragollama.entity.DocumentJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Сервис-исполнитель, отвечающий за выполнение ресурсоемкой логики
 * по пакетной обработке и индексации документов.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final DocumentJobService documentJobService;
    private final VectorStore vectorStore;
    private final TokenTextSplitter tokenTextSplitter;
    private final VectorCacheService vectorCacheService;

    /**
     * Асинхронно обрабатывает пакет задач по индексации документов.
     * Метод собирает все чанки со всех документов в пакете и выполняет
     * одну операцию вставки в {@link VectorStore}. В случае индивидуальных
     * ошибок при обработке документа, он помечается как FAILED, но обработка пакета продолжается.
     *
     * @param jobs Список задач, которые необходимо обработать.
     */
    @Async("applicationTaskExecutor")
    public void processBatch(List<DocumentJob> jobs) {
        String batchId = UUID.randomUUID().toString().substring(0, 8);
        try (MDC.MDCCloseable mdc = MDC.putCloseable("requestId", "ingest-batch-" + batchId)) {
            log.info("Начинается обработка пакета из {} документов.", jobs.size());
            List<Document> allChunks = new ArrayList<>();
            List<UUID> successfulJobIds = new ArrayList<>();
            for (DocumentJob job : jobs) {
                try {
                    Document document = new Document(
                            job.getTextContent(),
                            Map.of("source", job.getSourceName(), "documentId", job.getId().toString()));
                    List<Document> chunks = tokenTextSplitter.apply(List.of(document));
                    allChunks.addAll(chunks);
                    successfulJobIds.add(job.getId());
                } catch (Exception e) {
                    log.error("Ошибка при обработке документа в пакете. Job ID: {}", job.getId(), e);
                    documentJobService.markAsFailed(job.getId(), e.getMessage());
                }
            }

            if (!allChunks.isEmpty()) {
                try {
                    log.info("Добавление {} чанков в VectorStore для пакета {}.", allChunks.size(), batchId);
                    vectorStore.add(allChunks);
                    documentJobService.markBatchAsCompleted(successfulJobIds);
                    vectorCacheService.evictAll(); // Очищаем кэш после успешной вставки
                    log.info("Пакет {} успешно обработан.", batchId);
                } catch (Exception e) {
                    log.error("Критическая ошибка при добавлении чанков в VectorStore для пакета {}.", batchId, e);
                    // Помечаем все успешно обработанные до этого момента задачи как проваленные
                    successfulJobIds.forEach(id -> documentJobService.markAsFailed(id, "Ошибка VectorStore: " + e.getMessage()));
                }
            } else if (!jobs.isEmpty()) {
                log.warn("Все документы в пакете {} не удалось обработать. Пакет пуст.", batchId);
            }

        } catch (Exception e) {
            log.error("Непредвиденная ошибка в асинхронном обработчике пакета {}", batchId, e);
        }
    }
}
