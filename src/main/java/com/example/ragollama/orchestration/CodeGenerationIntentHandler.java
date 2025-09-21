package com.example.ragollama.orchestration;

import com.example.ragollama.agent.codegeneration.domain.CodeGenerationService;
import com.example.ragollama.agent.routing.QueryIntent;
import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.dto.UniversalSyncResponse;
import com.example.ragollama.orchestration.handlers.IntentHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Обработчик, реализующий логику для намерения {@link QueryIntent#CODE_GENERATION}.
 */
@Service
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
    public CompletableFuture<UniversalSyncResponse> handleSync(UniversalRequest request) {
        // Логика для синхронного ответа остается прежней, но также должна сохранять историю
        return dialogManager.startTurn(request.sessionId(), request.query(), MessageRole.USER)
                .thenCompose(turnContext ->
                        codeGenerationService.generateCode(request.toCodeGenerationRequest())
                                .flatMap(response -> Mono.fromFuture(
                                        dialogManager.endTurn(turnContext.sessionId(), response.generatedCode(), MessageRole.ASSISTANT)
                                                .thenApply(v -> UniversalSyncResponse.from(response, canHandle()))
                                )).toFuture()
                );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Flux<UniversalResponse> handleStream(UniversalRequest request) {
        // Шаг 1: Начинаем "ход" в диалоге, получаем ID сессии и историю
        return Mono.fromFuture(() -> dialogManager.startTurn(request.sessionId(), request.query(), MessageRole.USER))
                .flatMapMany(turnContext -> {
                    // Используем StringBuilder для накопления полного ответа для сохранения
                    final StringBuilder fullResponseBuilder = new StringBuilder();
                    // Шаг 2: Запускаем потоковую генерацию кода
                    return codeGenerationService.generateCodeStream(request.toCodeGenerationRequest())
                            // Шаг 3: По мере поступления фрагментов, добавляем их в StringBuilder
                            .doOnNext(fullResponseBuilder::append)
                            // Шаг 4: После завершения потока, сохраняем полный ответ в историю
                            .doOnComplete(() -> {
                                String fullResponse = fullResponseBuilder.toString();
                                if (!fullResponse.isBlank()) {
                                    dialogManager.endTurn(turnContext.sessionId(), fullResponse, MessageRole.ASSISTANT);
                                }
                            })
                            .map(UniversalResponse::from);
                });
    }
}
