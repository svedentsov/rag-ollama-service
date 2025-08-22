package com.example.ragollama.ingestion.consumer;

import com.example.ragollama.ingestion.cleaning.DataCleaningService;
import com.example.ragollama.ingestion.domain.DocumentJobService;
import com.example.ragollama.qaagent.config.RabbitMqConfig;
import com.example.ragollama.shared.caching.VectorCacheService;
import com.example.ragollama.shared.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Основной "воркер" конвейера индексации.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentProcessingConsumer {

    private final DocumentJobService documentJobService;
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
     * Обрабатывает событие {@link JobBatchConsumer.ProcessDocumentJobEvent}.
     */
    @RabbitListener(queues = RabbitMqConfig.DOCUMENT_PROCESSING_QUEUE)
    public void processDocument(@Payload JobBatchConsumer.ProcessDocumentJobEvent event) {
        try (MDC.MDCCloseable mdc = MDC.putCloseable("requestId", "ingest-job-" + event.jobId())) {
            log.info("Начата обработка документа '{}' (Job ID: {}).", event.sourceName(), event.jobId());

            processJob(event)
                    .collectList()
                    .flatMap(this::addChunksToVectorStore)
                    .doOnSuccess(v -> {
                        documentJobService.markBatchAsCompleted(List.of(event.jobId()));
                        log.info("Документ '{}' (Job ID: {}) успешно обработан и проиндексирован.", event.sourceName(), event.jobId());
                    })
                    .doOnError(e -> {
                        log.error("Критическая ошибка при обработке документа. Job ID: {}", event.jobId(), e);
                        documentJobService.markAsFailed(event.jobId(), "Критическая ошибка: " + e.getMessage());
                    })
                    .subscribe();

        } catch (Exception e) {
            log.error("Непредвиденная синхронная ошибка в обработчике документа. Job ID: {}", event.jobId(), e);
            documentJobService.markAsFailed(event.jobId(), "Непредвиденная ошибка: " + e.getMessage());
        }
    }

    /**
     * Выполняет очистку, разбивку и обогащение документа.
     */
    private Flux<Document> processJob(JobBatchConsumer.ProcessDocumentJobEvent event) {
        String cleanedText = dataCleaningService.cleanDocumentText(event.textContent());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", event.sourceName());
        metadata.put("documentId", event.jobId().toString());
        if (event.metadata() != null) {
            metadata.putAll(event.metadata());
        }

        Document document = new Document(cleanedText, metadata);
        List<Document> chunks = tokenTextSplitter.apply(List.of(document));
        log.debug("Создано {} чанков для документа '{}'.", chunks.size(), event.sourceName());

        return Flux.fromIterable(chunks)
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(this::generateSummaryForChunk)
                .sequential();
    }

    /**
     * Асинхронно генерирует краткое содержание для чанка.
     */
    private Mono<Document> generateSummaryForChunk(Document chunk) {
        String promptString = INGEST_SUMMARY_PROMPT.render(Map.of("chunk_text", chunk.getText()));
        CompletableFuture<String> summaryFuture = llmClient.callChat(new Prompt(promptString));

        return Mono.fromFuture(summaryFuture)
                .map(summary -> {
                    chunk.getMetadata().put("summary", summary);
                    return chunk;
                })
                .doOnError(e -> log.warn("Не удалось сгенерировать summary. Ошибка: {}", e.getMessage()))
                .onErrorReturn(chunk);
    }

    /**
     * Добавляет чанки в векторное хранилище.
     */
    private Mono<Void> addChunksToVectorStore(List<Document> chunks) {
        if (chunks.isEmpty()) {
            log.warn("Нет чанков для добавления в VectorStore.");
            return Mono.empty();
        }
        log.info("Добавление {} чанков в VectorStore.", chunks.size());
        return Mono.fromRunnable(() -> {
                    vectorStore.add(chunks);
                    vectorCacheService.evictAll();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
