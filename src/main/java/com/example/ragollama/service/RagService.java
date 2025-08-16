package com.example.ragollama.service;

import com.example.ragollama.dto.RagQueryRequest;
import com.example.ragollama.dto.RagQueryResponse;
import com.example.ragollama.dto.StreamingResponsePart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-фасад и "чистый" оркестратор, управляющий полным циклом RAG-запроса.
 * <p>
 * Этот класс является основной точкой входа для RAG-логики из контроллеров.
 * Его единственная ответственность - координировать вызовы специализированных
 * сервисов в правильной последовательности, не содержа бизнес-логики.
 * <ol>
 *     <li>{@link PromptGuardService} для проверки безопасности.</li>
 *     <li>{@link RetrievalService} для извлечения документов (гибридный поиск).</li>
 *     <li>{@link AugmentationService} для сборки промпта.</li>
 *     <li>{@link GenerationService} для генерации финального ответа.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final RetrievalService retrievalService;
    private final AugmentationService augmentationService;
    private final GenerationService generationService;
    private final PromptGuardService promptGuardService;

    /**
     * Обрабатывает RAG-запрос асинхронно, возвращая полный сгенерированный ответ.
     *
     * @param request DTO с запросом пользователя.
     * @return {@link CompletableFuture} с финальным {@link RagQueryResponse}.
     */
    public CompletableFuture<RagQueryResponse> queryAsync(RagQueryRequest request) {
        log.info("Получен асинхронный RAG-запрос: '{}'", request.query());
        promptGuardService.checkForInjection(request.query());
        return retrievalService.retrieveAndFuse(request.query(), request.topK(), request.similarityThreshold())
                .thenCompose(documents -> {
                    Prompt prompt = augmentationService.augment(documents, request.query());
                    return generationService.generate(prompt, documents);
                });
    }

    /**
     * Обрабатывает RAG-запрос и возвращает ответ в виде реактивного потока
     * структурированных событий {@link StreamingResponsePart}.
     *
     * @param request DTO с запросом пользователя.
     * @return {@link Flux}, который эмитит части сгенерированного ответа в виде структурированных DTO.
     */
    public Flux<StreamingResponsePart> queryStream(RagQueryRequest request) {
        log.info("Получен потоковый RAG-запрос: '{}'", request.query());
        promptGuardService.checkForInjection(request.query());
        Mono<List<Document>> documentsMono = Mono.fromFuture(() ->
                retrievalService.retrieveAndFuse(request.query(), request.topK(), request.similarityThreshold()));
        return documentsMono.flatMapMany((List<Document> documents) -> {
            Prompt prompt = augmentationService.augment(documents, request.query());
            return generationService.generateStructuredStream(prompt, documents);
        });
    }
}
