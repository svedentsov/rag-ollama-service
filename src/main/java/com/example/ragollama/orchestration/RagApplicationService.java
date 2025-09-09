package com.example.ragollama.orchestration;

import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.rag.api.dto.RagQueryRequest;
import com.example.ragollama.rag.api.dto.RagQueryResponse;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.pipeline.RagPipelineOrchestrator;
import com.example.ragollama.security.PromptGuardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Сервис прикладного уровня (Application Service), оркестрирующий бизнес-логику RAG.
 * Эта версия интегрирована с PromptGuardService для обеспечения безопасности на входе.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagApplicationService {

    private final RagPipelineOrchestrator ragPipelineOrchestrator;
    private final DialogManager dialogManager;
    private final PromptGuardService promptGuardService; // Новая зависимость

    /**
     * Асинхронно обрабатывает RAG-запрос, возвращая полный ответ.
     * Сначала выполняет проверку на Prompt Injection.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link CompletableFuture} с финальным {@link RagQueryResponse}.
     */
    public CompletableFuture<RagQueryResponse> processRagRequestAsync(RagQueryRequest request) {
        // Шаг 1: Проверка безопасности запроса. Если проверка не пройдена, Mono вернет ошибку.
        return promptGuardService.validate(request.query())
                // Шаг 2: Если проверка успешна, продолжаем выполнение основного конвейера.
                .then(Mono.fromFuture(() -> dialogManager.startTurn(request.sessionId(), request.query(), MessageRole.USER)))
                .flatMap(turnContext ->
                        Mono.fromFuture(() -> ragPipelineOrchestrator.queryAsync(
                                        request.query(),
                                        turnContext.history(),
                                        request.topK(),
                                        request.similarityThreshold(),
                                        turnContext.sessionId()
                                ))
                                .flatMap(ragAnswer ->
                                        Mono.fromFuture(() -> dialogManager.endTurn(turnContext.sessionId(), ragAnswer.answer(), MessageRole.ASSISTANT))
                                                .thenReturn(new RagQueryResponse(
                                                        ragAnswer.answer(),
                                                        ragAnswer.sourceCitations(),
                                                        turnContext.sessionId(),
                                                        ragAnswer.trustScoreReport(),
                                                        ragAnswer.validationReport()
                                                ))
                                )
                ).toFuture();
    }

    /**
     * Обрабатывает RAG-запрос в потоковом режиме с предварительной проверкой безопасности.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link Flux} со структурированными частями ответа.
     */
    public Flux<StreamingResponsePart> processRagRequestStream(RagQueryRequest request) {
        // Шаг 1: Проверка безопасности. thenMany() продолжит выполнение, только если Mono завершится успешно.
        return promptGuardService.validate(request.query())
                .thenMany(Flux.defer(() -> {
                    // Шаг 2: Запускаем основной конвейер, если проверка пройдена.
                    return Mono.fromFuture(() -> processRagRequestAsync(request))
                            .flatMapMany(response -> Flux.concat(
                                    Flux.just(new StreamingResponsePart.Content(response.answer())),
                                    Flux.just(new StreamingResponsePart.Sources(response.sourceCitations())),
                                    Flux.just(new StreamingResponsePart.Done("Успешно завершено"))
                            ));
                }));
    }
}
