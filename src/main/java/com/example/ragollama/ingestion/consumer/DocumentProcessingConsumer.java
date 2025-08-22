package com.example.ragollama.ingestion.consumer;

import com.example.ragollama.ingestion.cleaning.DataCleaningService;
import com.example.ragollama.ingestion.domain.DocumentJobRepository;
import com.example.ragollama.ingestion.domain.DocumentJobService;
import com.example.ragollama.ingestion.domain.model.DocumentJob;
import com.example.ragollama.qaagent.config.RabbitMqConfig;
import com.example.ragollama.shared.caching.VectorCacheService;
import com.example.ragollama.shared.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Основной "воркер" конвейера индексации, реализующий истинно пакетную обработку.
 * <p>
 * Этот потребитель слушает одно событие на целый пакет документов,
 * асинхронно обрабатывает их параллельно, а затем выполняет
 * одну высокопроизводительную пакетную вставку в векторное хранилище.
 * Такой подход значительно эффективнее и проще, чем обработка по одному сообщению.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentProcessingConsumer {

    private final DocumentJobService documentJobService;
    private final DocumentJobRepository documentJobRepository;
    private final VectorStore vectorStore;
    private final TokenTextSplitter tokenTextSplitter;
    private final VectorCacheService vectorCacheService;
    private final LlmClient llmClient;
    private final DataCleaningService dataCleaningService;

    private static final PromptTemplate INGEST_SUMMARY_PROMPT = new PromptTemplate("""
            Сделай очень краткое, но емкое резюме следующего текста.
            Сохрани ключевые факты, цифры и термины. Ответ должен быть на русском языке.
            Текст:
            {chunk_text}
            """);

    /**
     * DTO для события, инициирующего обработку пакета документов.
     *
     * @param jobIds Список ID задач в пакете.
     */
    public record BatchProcessingRequestedEvent(List<UUID> jobIds) {
    }

    /**
     * Обрабатывает событие {@link BatchProcessingRequestedEvent}, запуская полный
     * конвейер пакетной обработки.
     *
     * @param event Событие с идентификаторами задач пакета.
     */
    @RabbitListener(queues = RabbitMqConfig.DOCUMENT_PROCESSING_QUEUE)
    public void processDocumentBatch(@Payload BatchProcessingRequestedEvent event) {
        List<UUID> jobIds = event.jobIds();
        String batchId = UUID.randomUUID().toString().substring(0, 8);
        try (MDC.MDCCloseable mdc = MDC.putCloseable("requestId", "ingest-batch-" + batchId)) {
            log.info("Начата обработка пакета из {} документов.", jobIds.size());

            List<DocumentJob> jobs = documentJobRepository.findAllById(jobIds);

            Flux.fromIterable(jobs)
                    .parallel()
                    .runOn(Schedulers.boundedElastic())
                    .flatMap(this::processSingleJob)
                    .sequential()
                    .collectList()
                    .flatMap(this::addChunksToVectorStore)
                    .doOnSuccess(v -> {
                        documentJobService.markBatchAsCompleted(jobIds);
                        log.info("Пакет {} из {} документов успешно обработан и проиндексирован.", batchId, jobIds.size());
                    })
                    .doOnError(e -> {
                        log.error("Критическая ошибка при обработке пакета {}. Все задачи будут помечены как FAILED.", batchId, e);
                        jobIds.forEach(jobId -> documentJobService.markAsFailed(jobId, "Критическая ошибка пакета: " + e.getMessage()));
                    })
                    .subscribe();

        } catch (Exception e) {
            log.error("Непредвиденная синхронная ошибка в обработчике пакета {}. Job IDs: {}", batchId, jobIds, e);
            jobIds.forEach(jobId -> documentJobService.markAsFailed(jobId, "Непредвиденная ошибка: " + e.getMessage()));
        }
    }

    /**
     * Выполняет полный цикл обработки одного документа: очистка, разбиение, обогащение.
     *
     * @param job Задача для обработки.
     * @return Поток {@link Flux} с обработанными и обогащенными чанками.
     */
    private Flux<Document> processSingleJob(DocumentJob job) {
        try {
            String cleanedText = dataCleaningService.cleanDocumentText(job.getTextContent());

            // Метаданные из JSONB не поддерживаются в этой версии Spring AI, поэтому пока не добавляем
            Map<String, Object> metadata = Map.of(
                    "source", job.getSourceName(),
                    "documentId", job.getId().toString()
            );

            Document document = new Document(cleanedText, metadata);
            List<Document> chunks = tokenTextSplitter.apply(List.of(document));
            log.debug("Создано {} чанков для документа '{}' (Job ID: {}).", chunks.size(), job.getSourceName(), job.getId());

            return Flux.fromIterable(chunks)
                    .flatMap(this::generateSummaryForChunk);

        } catch (Exception e) {
            log.error("Ошибка при индивидуальной обработке документа. Job ID: {}", job.getId(), e);
            documentJobService.markAsFailed(job.getId(), e.getMessage());
            return Flux.empty();
        }
    }

    /**
     * Асинхронно генерирует краткое содержание (summary) для одного чанка с помощью LLM.
     *
     * @param chunk Документ-чанк для обогащения.
     * @return {@link Mono} с тем же чанком, но с добавленным полем 'summary' в метаданных.
     */
    private Mono<Document> generateSummaryForChunk(Document chunk) {
        String promptString = INGEST_SUMMARY_PROMPT.render(Map.of("chunk_text", chunk.getText()));
        CompletableFuture<String> summaryFuture = llmClient.callChat(new Prompt(promptString));

        return Mono.fromFuture(summaryFuture)
                .map(summary -> {
                    chunk.getMetadata().put("summary", summary);
                    return chunk;
                })
                .doOnError(e -> log.warn("Не удалось сгенерировать summary для чанка. Ошибка: {}", e.getMessage()))
                .onErrorReturn(chunk); // В случае ошибки возвращаем чанк без summary
    }

    /**
     * Выполняет одну пакетную вставку всех чанков в векторное хранилище.
     *
     * @param allChunksInBatch Список всех чанков, сгенерированных для пакета.
     * @return Пустой {@link Mono}, сигнализирующий о завершении операции.
     */
    private Mono<Void> addChunksToVectorStore(List<Document> allChunksInBatch) {
        if (allChunksInBatch.isEmpty()) {
            log.warn("В пакете не было сгенерировано чанков для добавления в VectorStore.");
            return Mono.empty();
        }
        log.info("Выполнение пакетной вставки {} чанков в VectorStore.", allChunksInBatch.size());
        return Mono.fromRunnable(() -> {
                    vectorStore.add(allChunksInBatch);
                    vectorCacheService.evictAll(); // Инвалидируем кэш после обновления данных
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
