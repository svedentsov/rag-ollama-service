package com.example.ragollama.ingestion.domain;

import com.example.ragollama.ingestion.domain.model.DocumentJob;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-исполнитель, отвечающий за выполнение ресурсоемкой логики
 * по пакетной обработке и индексации документов.
 * <p>
 * В этой версии добавлена логика генерации краткого содержания (summary)
 * для каждого чанка в процессе индексации.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final DocumentJobService documentJobService;
    private final VectorStore vectorStore;
    private final TokenTextSplitter tokenTextSplitter;
    private final VectorCacheService vectorCacheService;
    private final LlmClient llmClient; // Добавляем LLM клиент

    private static final PromptTemplate INGEST_SUMMARY_PROMPT = new PromptTemplate("""
            Сделай очень краткое, но емкое резюме следующего текста.
            Сохрани ключевые факты, цифры и термины. Ответ должен быть на русском языке.
            Текст:
            {chunk_text}
            """);

    @Async("applicationTaskExecutor")
    public void processBatch(List<DocumentJob> jobs) {
        String batchId = UUID.randomUUID().toString().substring(0, 8);
        try (MDC.MDCCloseable mdc = MDC.putCloseable("requestId", "ingest-batch-" + batchId)) {
            log.info("Начинается обработка пакета из {} документов.", jobs.size());

            // Создаем реактивный конвейер для обработки
            Flux.fromIterable(jobs)
                    .flatMap(this::processJob)
                    .collectList()
                    .flatMap(allChunks -> {
                        if (allChunks.isEmpty()) {
                            log.warn("В пакете {} не было сгенерировано ни одного чанка.", batchId);
                            return Mono.empty();
                        }
                        log.info("Добавление {} чанков в VectorStore для пакета {}.", allChunks.size(), batchId);
                        // Оборачиваем блокирующий вызов в Mono
                        return Mono.fromRunnable(() -> vectorStore.add(allChunks))
                                .doOnSuccess(v -> vectorCacheService.evictAll());
                    })
                    .doOnSuccess(v -> {
                        List<UUID> successfulJobIds = jobs.stream().map(DocumentJob::getId).toList();
                        documentJobService.markBatchAsCompleted(successfulJobIds);
                        log.info("Пакет {} успешно обработан.", batchId);
                    })
                    .doOnError(e -> {
                        log.error("Критическая ошибка при обработке пакета {}.", batchId, e);
                        jobs.forEach(job -> documentJobService.markAsFailed(job.getId(), "Критическая ошибка пакета: " + e.getMessage()));
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .block(); // Блокируем до завершения всей обработки пакета

        } catch (Exception e) {
            log.error("Непредвиденная ошибка в асинхронном обработчике пакета {}", batchId, e);
        }
    }

    private Flux<Document> processJob(DocumentJob job) {
        try {
            Document document = new Document(
                    job.getTextContent(),
                    Map.of("source", job.getSourceName(), "documentId", job.getId().toString()));
            List<Document> chunks = tokenTextSplitter.apply(List.of(document));

            // Для каждого чанка асинхронно генерируем summary
            return Flux.fromIterable(chunks)
                    .parallel()
                    .runOn(Schedulers.boundedElastic())
                    .flatMap(this::generateSummaryForChunk)
                    .sequential();
        } catch (Exception e) {
            log.error("Ошибка при обработке документа в пакете. Job ID: {}", job.getId(), e);
            documentJobService.markAsFailed(job.getId(), e.getMessage());
            return Flux.empty();
        }
    }

    private Mono<Document> generateSummaryForChunk(Document chunk) {
        String promptString = INGEST_SUMMARY_PROMPT.render(Map.of("chunk_text", chunk.getText()));
        CompletableFuture<String> summaryFuture = llmClient.callChat(new Prompt(promptString));

        return Mono.fromFuture(summaryFuture)
                .map(summary -> {
                    // Добавляем summary в метаданные
                    chunk.getMetadata().put("summary", summary);
                    return chunk;
                })
                .doOnError(e -> log.warn("Не удалось сгенерировать summary для чанка документа {}. Используем полный текст.", chunk.getMetadata().get("source")))
                .onErrorReturn(chunk); // В случае ошибки возвращаем чанк без summary
    }
}
