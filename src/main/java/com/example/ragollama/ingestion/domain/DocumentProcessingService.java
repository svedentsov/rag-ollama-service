package com.example.ragollama.ingestion.domain;

import com.example.ragollama.ingestion.cleaning.DataCleaningService;
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
 * Реализует полный конвейер подготовки данных:
 * 1. Очистка "сырого" текста от HTML и другого "шума".
 * 2. Разбиение чистого текста на чанки.
 * 3. Асинхронная генерация краткого содержания (summary) для каждого чанка.
 * 4. Пакетная вставка обогащенных документов в векторное хранилище.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

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
     * Асинхронно обрабатывает пакет задач по индексации документов.
     *
     * @param jobs Список задач, которые необходимо обработать.
     */
    @Async("applicationTaskExecutor")
    public void processBatch(List<DocumentJob> jobs) {
        String batchId = UUID.randomUUID().toString().substring(0, 8);
        try (MDC.MDCCloseable mdc = MDC.putCloseable("requestId", "ingest-batch-" + batchId)) {
            log.info("Начинается обработка пакета из {} документов.", jobs.size());

            // Создаем реактивный конвейер для обработки всех задач в пакете
            Flux.fromIterable(jobs)
                    .flatMap(this::processJob)
                    .collectList()
                    .flatMap(allChunks -> {
                        if (allChunks.isEmpty()) {
                            log.warn("В пакете {} не было сгенерировано ни одного чанка для индексации.", batchId);
                            return Mono.empty();
                        }
                        log.info("Добавление {} чанков в VectorStore для пакета {}.", allChunks.size(), batchId);
                        // Оборачиваем блокирующий вызов vectorStore.add в Mono для интеграции в реактивную цепочку
                        return Mono.fromRunnable(() -> vectorStore.add(allChunks))
                                .doOnSuccess(v -> {
                                    log.info("Очистка кэша векторного поиска после успешной индексации.");
                                    vectorCacheService.evictAll();
                                });
                    })
                    .doOnSuccess(v -> {
                        List<UUID> successfulJobIds = jobs.stream().map(DocumentJob::getId).toList();
                        documentJobService.markBatchAsCompleted(successfulJobIds);
                        log.info("Пакет {} успешно обработан.", batchId);
                    })
                    .doOnError(e -> {
                        log.error("Критическая ошибка при обработке пакета {}. Все задачи в пакете будут помечены как FAILED.", batchId, e);
                        jobs.forEach(job -> documentJobService.markAsFailed(job.getId(), "Критическая ошибка пакета: " + e.getMessage()));
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .block(); // Блокируем до завершения всей обработки пакета, так как метод @Async void

        } catch (Exception e) {
            log.error("Непредвиденная ошибка в асинхронном обработчике пакета {}", batchId, e);
        }
    }

    /**
     * Обрабатывает одну задачу (DocumentJob), выполняя очистку, разбивку и суммаризацию.
     *
     * @param job Задача для обработки.
     * @return Поток {@link Flux} с обработанными и обогащенными документами-чанками.
     */
    private Flux<Document> processJob(DocumentJob job) {
        try {
            // ШАГ 1: Очистка "сырого" текста
            log.debug("Job ID {}: Очистка текста...", job.getId());
            String cleanedText = dataCleaningService.cleanDocumentText(job.getTextContent());

            Document document = new Document(
                    cleanedText, // Используем очищенный текст
                    Map.of("source", job.getSourceName(), "documentId", job.getId().toString()));

            // ШАГ 2: Разбиение на чанки (уже очищенного текста)
            log.debug("Job ID {}: Разбиение на чанки...", job.getId());
            List<Document> chunks = tokenTextSplitter.apply(List.of(document));
            log.debug("Job ID {}: Создано {} чанков.", job.getId(), chunks.size());

            // ШАГ 3: Асинхронная генерация summary для каждого чанка
            return Flux.fromIterable(chunks)
                    .parallel()
                    .runOn(Schedulers.boundedElastic())
                    .flatMap(this::generateSummaryForChunk)
                    .sequential();
        } catch (Exception e) {
            log.error("Ошибка при индивидуальной обработке документа. Job ID: {}", job.getId(), e);
            documentJobService.markAsFailed(job.getId(), e.getMessage());
            return Flux.empty();
        }
    }

    /**
     * Асинхронно генерирует краткое содержание для одного чанка.
     *
     * @param chunk Документ-чанк.
     * @return {@link Mono} с тем же чанком, но с добавленным полем 'summary' в метаданных.
     */
    private Mono<Document> generateSummaryForChunk(Document chunk) {
        String promptString = INGEST_SUMMARY_PROMPT.render(Map.of("chunk_text", chunk.getText()));
        CompletableFuture<String> summaryFuture = llmClient.callChat(new Prompt(promptString));

        return Mono.fromFuture(summaryFuture)
                .map(summary -> {
                    // Добавляем summary в метаданные
                    chunk.getMetadata().put("summary", summary);
                    log.trace("Job ID {}: Успешно сгенерировано summary для чанка.", chunk.getMetadata().get("documentId"));
                    return chunk;
                })
                .doOnError(e -> log.warn("Не удалось сгенерировать summary для чанка документа {}. Используем полный текст. Ошибка: {}",
                        chunk.getMetadata().get("source"), e.getMessage()))
                .onErrorReturn(chunk); // В случае ошибки возвращаем чанк без summary
    }
}
