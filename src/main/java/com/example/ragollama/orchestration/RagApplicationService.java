package com.example.ragollama.orchestration;

import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.rag.api.dto.RagQueryRequest;
import com.example.ragollama.rag.api.dto.RagQueryResponse;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.pipeline.RagPipelineOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Сервис прикладного уровня (Application Service), оркестрирующий бизнес-логику RAG.
 * <p>
 * Эта версия была отрефакторена для использования централизованного {@link DialogManager},
 * что устраняет дублирование кода и соответствует принципам Clean Architecture.
 * Проверка безопасности теперь делегирована первому шагу RAG-конвейера.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagApplicationService {

    private final RagPipelineOrchestrator ragPipelineOrchestrator;
    private final DialogManager dialogManager;

    /**
     * Асинхронно обрабатывает RAG-запрос, возвращая полный ответ.
     * <p>
     * Конвейер выполнения:
     * <ol>
     *     <li>Вызов {@link DialogManager#startTurn} для получения контекста диалога.</li>
     *     <li>Запуск основного RAG-конвейера через {@link RagPipelineOrchestrator},
     *         который теперь включает шаг проверки безопасности.</li>
     *     <li>Вызов {@link DialogManager#endTurn} для асинхронного сохранения ответа.</li>
     *     <li>Формирование и возврат финального DTO {@link RagQueryResponse}.</li>
     * </ol>
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link CompletableFuture} с финальным {@link RagQueryResponse}.
     */
    public CompletableFuture<RagQueryResponse> processRagRequestAsync(RagQueryRequest request) {
        return dialogManager.startTurn(request.sessionId(), request.query(), MessageRole.USER)
                .thenCompose(turnContext ->
                        ragPipelineOrchestrator.queryAsync(
                                        request.query(),
                                        turnContext.history(),
                                        request.topK(),
                                        request.similarityThreshold(),
                                        turnContext.sessionId()
                                )
                                .thenCompose(ragAnswer ->
                                        dialogManager.endTurn(turnContext.sessionId(), ragAnswer.answer(), MessageRole.ASSISTANT)
                                                .thenApply(v -> new RagQueryResponse(
                                                        ragAnswer.answer(),
                                                        ragAnswer.sourceCitations(),
                                                        turnContext.sessionId(),
                                                        ragAnswer.trustScoreReport(),
                                                        ragAnswer.validationReport()
                                                ))
                                )
                );
    }

    /**
     * Обрабатывает RAG-запрос в потоковом режиме.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link Flux} со структурированными частями ответа.
     */
    public Flux<StreamingResponsePart> processRagRequestStream(RagQueryRequest request) {
        // Запускаем полный асинхронный конвейер и преобразуем его результат в поток
        return Mono.fromFuture(() -> processRagRequestAsync(request))
                .flatMapMany(response -> Flux.concat(
                        Flux.just(new StreamingResponsePart.Content(response.answer())),
                        Flux.just(new StreamingResponsePart.Sources(response.sourceCitations())),
                        Flux.just(new StreamingResponsePart.Done("Успешно завершено"))
                ));
    }
}
