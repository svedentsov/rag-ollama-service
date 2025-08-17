package com.example.ragollama.service;

import com.example.ragollama.dto.RagQueryRequest;
import com.example.ragollama.dto.RagQueryResponse;
import com.example.ragollama.dto.StreamingResponsePart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-фасад и "чистый" оркестратор, управляющий полным, многоэтапным RAG-конвейером.
 * <p>
 * Эта версия включает расширенное диагностическое логирование для отладки
 * этапов трансформации, извлечения и переранжирования, что позволяет
 * точно отслеживать поток данных и выявлять проблемы с релевантностью.
 * <p>
 * Конвейер включает следующие этапы:
 * <ol>
 *   <li><b>Query Transformation:</b> Улучшение исходного запроса с помощью LLM для повышения точности поиска.</li>
 *   <li><b>Retrieval:</b> Асинхронное извлечение релевантных документов из векторного хранилища.</li>
 *   <li><b>Re-ranking:</b> Пересортировка найденных документов для повышения релевантности наиболее важных фрагментов.</li>
 *   <li><b>Augmentation:</b> Динамическое построение промпта с использованием паттерна "Advisor" и управление контекстным окном.</li>
 *   <li><b>Generation:</b> Потоковая генерация ответа с помощью отказоустойчивого LLM-клиента.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final QueryTransformationService queryTransformationService;
    private final RetrievalService retrievalService;
    private final RerankingService rerankingService;
    private final AugmentationService augmentationService;
    private final GenerationService generationService;
    private final PromptGuardService promptGuardService;
    private final MetricService metricService;

    /**
     * Обрабатывает RAG-запрос асинхронно, возвращая полный сгенерированный ответ.
     *
     * @param request DTO с запросом пользователя.
     * @return {@link CompletableFuture}, который по завершении всего конвейера будет
     *         содержать финальный объект {@link RagQueryResponse}.
     */
    public CompletableFuture<RagQueryResponse> queryAsync(RagQueryRequest request) {
        log.info("Получен асинхронный RAG-запрос: '{}'", request.query());
        promptGuardService.checkForInjection(request.query());
        return metricService.recordTimer("rag.requests.async", () ->
                buildRagFlow(request)
                        .reduce(new RagQueryResponse("", List.of()), (acc, part) -> {
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
     * @return {@link Flux}, который эмитит части сгенерированного ответа в реальном времени.
     */
    public Flux<StreamingResponsePart> queryStream(RagQueryRequest request) {
        log.info("Получен потоковый RAG-запрос: '{}'", request.query());
        promptGuardService.checkForInjection(request.query());
        return metricService.recordTimer("rag.requests.stream", () -> buildRagFlow(request));
    }

    /**
     * Строит и возвращает основную реактивную цепочку RAG-конвейера с диагностикой.
     *
     * @param request DTO с запросом пользователя.
     * @return {@link Flux} со структурированными частями ответа.
     */
    private Flux<StreamingResponsePart> buildRagFlow(RagQueryRequest request) {
        return queryTransformationService.transform(request.query())
                .doOnNext(transformedQuery -> log.info("[DIAGNOSTIC] Запрос '{}' трансформирован в '{}'", request.query(), transformedQuery))
                .flatMap(transformedQuery -> retrievalService.retrieveDocuments(transformedQuery, request.topK(), request.similarityThreshold()))
                .doOnNext(documents -> log.info("[DIAGNOSTIC] Этап Retrieval: Найдено {} документов.", documents.size()))
                .map(documents -> {
                    var reranked = rerankingService.rerank(documents, request.query());
                    log.info("[DIAGNOSTIC] Этап Re-ranking: Осталось {} документов после переранжирования.", reranked.size());
                    return reranked;
                })
                .flatMapMany(rerankedDocuments -> {
                    if (rerankedDocuments.isEmpty()) {
                        log.warn("Не найдено релевантных документов для запроса '{}' после всех этапов. Генерация будет основана на знаниях LLM.", request.query());
                    }
                    Prompt prompt = augmentationService.augment(rerankedDocuments, request.query());
                    return generationService.generateStructuredStream(prompt, rerankedDocuments);
                })
                .onErrorResume(e -> {
                    log.error("Критическая ошибка в RAG-потоке для запроса '{}': {}", request.query(), e.getMessage(), e);
                    return Flux.just(new StreamingResponsePart.Error("Произошла внутренняя ошибка при обработке вашего запроса."));
                });
    }
}
