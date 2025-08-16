package com.example.ragollama.service;

import com.example.ragollama.dto.RagQueryRequest;
import com.example.ragollama.dto.RagQueryResponse;
import com.example.ragollama.dto.StreamingResponsePart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-фасад и "чистый" оркестратор, управляющий полным циклом RAG-запроса.
 * Реализует единую реактивную цепочку для обработки запроса, которая
 * затем адаптируется для предоставления как потокового, так и полного ответа.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final RetrievalService retrievalService;
    private final AugmentationService augmentationService;
    private final GenerationService generationService;
    private final PromptGuardService promptGuardService;
    private final MetricService metricService;

    /**
     * Обрабатывает RAG-запрос асинхронно, возвращая полный сгенерированный ответ.
     * Весь процесс выполнения этого метода измеряется таймером {@code rag.requests.async}
     * для точного мониторинга производительности.
     *
     * @param request DTO с запросом пользователя.
     * @return {@link CompletableFuture} с финальным {@link RagQueryResponse}.
     */
    public CompletableFuture<RagQueryResponse> queryAsync(RagQueryRequest request) {
        log.info("Получен асинхронный RAG-запрос: '{}'", request.query());
        promptGuardService.checkForInjection(request.query());
        return metricService.recordTimer("rag.requests.async", () ->
                buildRagFlow(request)
                        .reduce(new RagQueryResponse("", new ArrayList<>()), (acc, part) -> {
                            if (part instanceof StreamingResponsePart.Content content) {
                                return new RagQueryResponse(acc.answer() + content.text(), acc.sourceCitations());
                            }
                            if (part instanceof StreamingResponsePart.Sources sources) {
                                return new RagQueryResponse(acc.answer(), sources.sources());
                            }
                            return acc;
                        })
                        .toFuture()
        );
    }

    /**
     * Обрабатывает RAG-запрос и возвращает ответ в виде реактивного потока
     * структурированных событий {@link StreamingResponsePart}.
     *
     * @param request DTO с запросом пользователя.
     * @return {@link Flux}, который эмитит части сгенерированного ответа.
     */
    public Flux<StreamingResponsePart> queryStream(RagQueryRequest request) {
        log.info("Получен потоковый RAG-запрос: '{}'", request.query());
        promptGuardService.checkForInjection(request.query());
        return buildRagFlow(request);
    }

    /**
     * Строит и возвращает основную реактивную цепочку RAG-конвейера.
     *
     * @param request DTO с запросом пользователя.
     * @return {@link Flux} со структурированными частями ответа.
     */
    private Flux<StreamingResponsePart> buildRagFlow(RagQueryRequest request) {
        // Шаг 1: Асинхронное извлечение документов (Retrieval)
        return retrievalService.retrieveDocuments(request.query(), request.topK(), request.similarityThreshold())
                .flatMapMany(documents -> {
                    // Шаг 2: Сборка промпта (Augmentation)
                    Prompt prompt = augmentationService.augment(documents, request.query());
                    // Шаг 3: Потоковая генерация ответа (Generation)
                    return generationService.generateStructuredStream(prompt, documents);
                })
                .onErrorResume(e -> {
                    log.error("Ошибка в RAG-потоке для запроса '{}': {}", request.query(), e.getMessage(), e);
                    return Flux.just(new StreamingResponsePart.Error("Произошла внутренняя ошибка при обработке вашего запроса."));
                });
    }
}
