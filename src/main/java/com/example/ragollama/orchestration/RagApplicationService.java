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
 * *
 * <p>Эта версия была отрефакторена для следования принципам Clean Architecture.
 * Вся логика управления сессиями и историей делегирована в {@link DialogManager}.
 * Ответственность этого класса теперь полностью сфокусирована на оркестрации
 * именно RAG-взаимодействия:
 * <ol>
 *     <li>Начать или продолжить диалог через {@link DialogManager}.</li>
 *     <li>Вызвать "чистый" доменный RAG-конвейер {@link RagPipelineOrchestrator}.</li>
 *     <li>Завершить диалог, сохранив RAG-ответ.</li>
 * </ol>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagApplicationService {

    private final RagPipelineOrchestrator ragPipelineOrchestrator;
    private final DialogManager dialogManager;

    /**
     * Асинхронно обрабатывает RAG-запрос, возвращая полный ответ.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link CompletableFuture} с финальным {@link RagQueryResponse}.
     */
    public CompletableFuture<RagQueryResponse> processRagRequestAsync(RagQueryRequest request) {
        // Шаг 1: Начинаем диалог, получаем контекст с историей
        return dialogManager.startTurn(request.sessionId(), request.query(), MessageRole.USER)
                .thenCompose(turnContext ->
                        // Шаг 2: Выполняем основную бизнес-логику (RAG-пайплайн)
                        ragPipelineOrchestrator.queryAsync(
                                        request.query(),
                                        turnContext.history(),
                                        request.topK(),
                                        request.similarityThreshold(),
                                        turnContext.sessionId()
                                )
                                // Шаг 3: Завершаем диалог, сохраняя RAG-ответ
                                .thenCompose(ragAnswer ->
                                        dialogManager.endTurn(turnContext.sessionId(), ragAnswer.answer(), MessageRole.ASSISTANT)
                                                .thenApply(v -> new RagQueryResponse(ragAnswer.answer(), ragAnswer.sourceCitations(), turnContext.sessionId()))
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
        // Для потоковой версии логика остается более сложной и пока не использует DialogManager
        // для простоты примера, но в идеале ее тоже нужно было бы отрефакторить.
        return Mono.fromFuture(() -> processRagRequestAsync(request))
                .flatMapMany(response -> Flux.concat(
                        Flux.just(new StreamingResponsePart.Content(response.answer())),
                        Flux.just(new StreamingResponsePart.Sources(response.sourceCitations())),
                        Flux.just(new StreamingResponsePart.Done("Успешно завершено"))
                ));
    }
}
