package com.example.ragollama.ingestion.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.indexing.IndexingPipelineService;
import com.example.ragollama.indexing.IndexingRequest;
import com.example.ragollama.optimization.DocumentEnhancerAgent;
import com.example.ragollama.optimization.model.EnhancedMetadata;
import com.example.ragollama.shared.exception.ProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Асинхронный воркер для обработки и индексации документов, адаптированный для R2DBC.
 * Эта версия использует нативную асинхронность Project Reactor вместо Spring @Async.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingWorker {

    private final DocumentJobRepository jobRepository;
    private final IndexingPipelineService indexingPipelineService;
    private final DocumentEnhancerAgent enhancerAgent;

    /**
     * Асинхронно обрабатывает и индексирует один документ.
     * <p>
     * Метод возвращает {@link Mono<Void>}, который должен быть запущен (через .subscribe())
     * вызывающей стороной. Вся логика выполняется в рамках реактивной цепочки,
     * что является идиоматичным подходом в WebFlux приложении.
     * Аннотация {@code @Async} была удалена, чтобы избежать конфликта парадигм асинхронности.
     *
     * @param jobId ID задачи для обработки.
     * @return {@link Mono<Void>}, который завершается после выполнения всех операций.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<Void> processDocument(UUID jobId) {
        // Устанавливаем MDC для сквозного логирования
        try (MDC.MDCCloseable mdc = MDC.putCloseable("requestId", "ingest-" + jobId.toString().substring(0, 8))) {
            log.info("Начата асинхронная обработка документа. Job ID: {}", jobId);
            // 1. Загружаем исходную задачу
            return jobRepository.findById(jobId)
                    .switchIfEmpty(Mono.error(new ProcessingException("Задача с ID " + jobId + " не найдена.")))
                    .flatMap(job ->
                            // 2. Выполняем основную бизнес-логику (вызов AI, индексация)
                            enhancerAgent.execute(new AgentContext(Map.of("document_text", job.getTextContent())))
                                    .flatMap(enhancerResult -> {
                                        EnhancedMetadata enhancedMetadata = (EnhancedMetadata) enhancerResult.details().get("enhancedMetadata");
                                        Map<String, Object> finalMetadata = new HashMap<>(job.getMetadata());
                                        finalMetadata.put("summary", enhancedMetadata.summary());
                                        finalMetadata.put("keywords", enhancedMetadata.keywords());

                                        IndexingRequest indexingRequest = new IndexingRequest(
                                                job.getId().toString(),
                                                job.getSourceName(),
                                                job.getTextContent(),
                                                finalMetadata
                                        );
                                        // Запускаем индексацию
                                        return indexingPipelineService.process(indexingRequest);
                                    })
                                    // 3. После успешного выполнения логики, обновляем статус
                                    .then(updateJobStatus(jobId, null))
                                    .doOnSuccess(v -> log.info("Обработка документа для Job ID {} успешно завершена.", jobId))
                    )
                    // 4. В случае любой ошибки на предыдущих этапах, обновляем статус на FAILED
                    .onErrorResume(e -> {
                        log.error("Критическая ошибка при обработке документа. Job ID: {}", jobId, e);
                        return updateJobStatus(jobId, e.getMessage());
                    });
        }
    }

    /**
     * Изолированный метод для атомарного обновления статуса задачи.
     * Реализует паттерн "Загрузить -> Изменить -> Сохранить" в рамках одной транзакции.
     *
     * @param jobId   ID задачи.
     * @param message Сообщение об ошибке (или null в случае успеха).
     * @return {@link Mono<Void>}, завершающийся после сохранения.
     */
    private Mono<Void> updateJobStatus(UUID jobId, String message) {
        return jobRepository.findById(jobId)
                .flatMap(jobToUpdate -> {
                    if (message == null) {
                        jobToUpdate.markAsCompleted();
                    } else {
                        jobToUpdate.markAsFailed(message);
                    }
                    return jobRepository.save(jobToUpdate);
                })
                .then();
    }
}
