package com.example.ragollama.orchestration;

import com.example.ragollama.agent.codegeneration.domain.CodeGenerationService;
import com.example.ragollama.agent.routing.QueryIntent;
import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.dto.UniversalSyncResponse;
import com.example.ragollama.orchestration.handlers.IntentHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * Обработчик для намерения "Генерация кода", адаптированный для R2DBC и реактивного стека.
 * <p>
 * Эта версия использует оператор {@code doFinally} для гарантированного сохранения
 * результата в базу данных, даже если поток был прерван клиентом.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CodeGenerationIntentHandler implements IntentHandler {

    private final CodeGenerationService codeGenerationService;
    private final DialogManager dialogManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryIntent canHandle() {
        return QueryIntent.CODE_GENERATION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<UniversalSyncResponse> handleSync(UniversalRequest request, UUID taskId) {
        return dialogManager.startTurn(request.sessionId(), request.query(), MessageRole.USER)
                .flatMap(turnContext ->
                        codeGenerationService.generateCode(request.toCodeGenerationRequest())
                                .flatMap(response ->
                                        dialogManager.endTurn(turnContext.sessionId(), turnContext.userMessageId(), response.generatedCode(), MessageRole.ASSISTANT, taskId)
                                                .thenReturn(UniversalSyncResponse.from(response, canHandle()))
                                )
                ).toFuture();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Flux<UniversalResponse> handleStream(UniversalRequest request, UUID taskId) {
        return dialogManager.startTurn(request.sessionId(), request.query(), MessageRole.USER)
                .flatMapMany(turnContext -> {
                    final StringBuilder fullResponseBuilder = new StringBuilder();
                    return codeGenerationService.generateCodeStream(request.toCodeGenerationRequest())
                            .map(UniversalResponse::from) // Сначала мапим в UniversalResponse
                            .doOnNext(part -> {
                                if (part instanceof UniversalResponse.Content content) {
                                    fullResponseBuilder.append(content.text());
                                }
                            })
                            .doFinally(signalType -> {
                                // Гарантированное сохранение при штатном завершении или отмене клиентом
                                if (signalType == SignalType.ON_COMPLETE || signalType == SignalType.CANCEL) {
                                    String responseToSave = fullResponseBuilder.toString();
                                    if (!responseToSave.isBlank()) {
                                        dialogManager.endTurn(turnContext.sessionId(), turnContext.userMessageId(), responseToSave, MessageRole.ASSISTANT, taskId)
                                                .subscribe(
                                                        null, // onNext не нужен
                                                        error -> log.error("Ошибка при сохранении (частичного) ответа генерации кода для сессии {}", turnContext.sessionId(), error)
                                                );
                                    }
                                }
                            })
                            .concatWith(Mono.just(new UniversalResponse.Done("Генерация завершена.")))
                            .onErrorResume(e -> {
                                Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                                // Не логируем отмену как ошибку, так как это штатное поведение
                                if (cause instanceof CancellationException || cause instanceof IOException) {
                                    log.warn("Поток генерации кода был прерван клиентом: {}", cause.getMessage());
                                } else {
                                    log.error("Ошибка в потоке генерации кода: {}", e.getMessage(), e);
                                }
                                // При любой ошибке или отмене, поток просто завершается.
                                // `doFinally` позаботится о сохранении.
                                return Flux.empty();
                            });
                });
    }
}
