package com.example.ragollama.service;

import com.example.ragollama.dto.RagQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-оркестратор, который управляет выполнением полного RAG-конвейера.
 * Этот класс не содержит бизнес-логики этапов, а только вызывает
 * специализированные сервисы в правильной последовательности,
 * составляя из них цепочку асинхронных вычислений.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagOrchestrationService {

    private final RetrievalService retrievalService;
    private final AugmentationService augmentationService;
    private final GenerationService generationService;

    /**
     * Выполняет полный RAG-пайплайн и возвращает готовый ответ.
     * Последовательность шагов:
     * 1. Асинхронно извлечь и переранжировать документы (Retrieval).
     * 2. Синхронно собрать промпт на основе полученных документов (Augmentation).
     * 3. Асинхронно сгенерировать ответ с помощью LLM (Generation).
     *
     * @param query               Вопрос пользователя.
     * @param topK                Количество документов для поиска.
     * @param similarityThreshold Порог схожести.
     * @return {@link CompletableFuture} с финальным {@link RagQueryResponse}.
     */
    public CompletableFuture<RagQueryResponse> execute(String query, int topK, double similarityThreshold) {
        return retrievalService.retrieveAndRerank(query, topK, similarityThreshold)
                .thenCompose(documents -> {
                    if (documents.isEmpty()) {
                        log.warn("На этапе Retrieval не найдено релевантных документов для запроса: '{}'", query);
                        String emptyContextAnswer = "Извините, я не смог найти релевантную информацию в базе знаний по вашему вопросу.";
                        return CompletableFuture.completedFuture(new RagQueryResponse(emptyContextAnswer, List.of()));
                    }
                    var prompt = augmentationService.augment(documents, query);
                    return generationService.generate(prompt, documents);
                });
    }

    /**
     * Выполняет RAG-пайплайн и возвращает ответ в виде реактивного потока.
     * Логика аналогична {@link #execute}, но адаптирована для реактивных стримов.
     *
     * @param query               Вопрос пользователя.
     * @param topK                Количество документов для поиска.
     * @param similarityThreshold Порог схожести.
     * @return {@link Flux} с частями ответа от LLM.
     */
    public Flux<String> executeStream(String query, int topK, double similarityThreshold) {
        // Конвертируем CompletableFuture из этапа Retrieval в реактивный тип Mono.
        return Mono.fromFuture(() -> retrievalService.retrieveAndRerank(query, topK, similarityThreshold))
                .flatMapMany(documents -> {
                    if (documents.isEmpty()) {
                        log.warn("На этапе Retrieval не найдено релевантных документов для потокового запроса: '{}'", query);
                        return Flux.just("Извините, я не смог найти релевантную информацию в базе знаний по вашему вопросу.");
                    }
                    var prompt = augmentationService.augment(documents, query);
                    return generationService.generateStream(prompt);
                });
    }
}
