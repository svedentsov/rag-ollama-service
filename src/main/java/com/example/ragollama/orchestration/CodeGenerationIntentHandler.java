// src/main/java/com/example/ragollama/orchestration/CodeGenerationIntentHandler.java
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class CodeGenerationIntentHandler implements IntentHandler {

    private final CodeGenerationService codeGenerationService;
    private final DialogManager dialogManager;

    @Override
    public QueryIntent canHandle() {
        return QueryIntent.CODE_GENERATION;
    }

    @Override
    public CompletableFuture<UniversalSyncResponse> handleSync(UniversalRequest request) {
        return dialogManager.startTurn(request.sessionId(), request.query(), MessageRole.USER)
                .thenCompose(turnContext ->
                        codeGenerationService.generateCode(request.toCodeGenerationRequest())
                                .flatMap(response -> Mono.fromFuture(
                                        dialogManager.endTurn(turnContext.sessionId(), response.generatedCode(), MessageRole.ASSISTANT)
                                                .thenApply(v -> UniversalSyncResponse.from(response, canHandle()))
                                )).toFuture()
                );
    }

    @Override
    public Flux<UniversalResponse> handleStream(UniversalRequest request) {
        return Mono.fromFuture(() -> dialogManager.startTurn(request.sessionId(), request.query(), MessageRole.USER))
                .flatMapMany(turnContext -> {
                    final StringBuilder fullResponseBuilder = new StringBuilder();
                    return codeGenerationService.generateCodeStream(request.toCodeGenerationRequest())
                            .doOnNext(fullResponseBuilder::append)
                            .doOnComplete(() -> {
                                String fullResponse = fullResponseBuilder.toString();
                                if (!fullResponse.isBlank()) {
                                    dialogManager.endTurn(turnContext.sessionId(), fullResponse, MessageRole.ASSISTANT);
                                }
                            })
                            .map(UniversalResponse::from)
                            .onErrorResume(e -> {
                                Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                                if (cause instanceof CancellationException || cause instanceof IOException) {
                                    log.warn("Поток генерации кода был прерван клиентом: {}", cause.getMessage());
                                    return Flux.empty();
                                }
                                log.error("Ошибка в потоке генерации кода: {}", e.getMessage(), e);
                                return Flux.just(new UniversalResponse.Error("Ошибка при генерации кода: " + e.getMessage()));
                            });
                });
    }
}