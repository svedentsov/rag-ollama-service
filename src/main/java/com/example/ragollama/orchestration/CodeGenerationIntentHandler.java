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
                            .concatWith( // Затем конкатенируем с Mono<UniversalResponse>
                                    Mono.defer(() -> {
                                        String fullResponse = fullResponseBuilder.toString();
                                        if (!fullResponse.isBlank()) {
                                            return dialogManager.endTurn(turnContext.sessionId(), turnContext.userMessageId(), fullResponse, MessageRole.ASSISTANT, taskId)
                                                    .thenReturn(new UniversalResponse.Done("Сохранено на сервере."));
                                        }
                                        return Mono.just(new UniversalResponse.Done("Завершено без сохранения."));
                                    })
                            )
                            .onErrorResume(e -> {
                                Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                                if (cause instanceof CancellationException || cause instanceof IOException) {
                                    log.warn("Поток генерации кода был прерван клиентом: {}", cause.getMessage());
                                    String partialResponse = fullResponseBuilder.toString();
                                    if (!partialResponse.isBlank()) {
                                        return dialogManager.endTurn(turnContext.sessionId(), turnContext.userMessageId(), partialResponse, MessageRole.ASSISTANT, taskId)
                                                .thenReturn(new UniversalResponse.Done("Частично сохранено на сервере."))
                                                .flux();
                                    }
                                    return Flux.empty();
                                }
                                log.error("Ошибка в потоке генерации кода: {}", e.getMessage(), e);
                                return Flux.just(new UniversalResponse.Error("Ошибка при генерации кода: " + e.getMessage()));
                            });
                });
    }
}
