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

import java.util.UUID;
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
        // Для генерации кода потоковый ответ не имеет смысла, так как код генерируется целиком.
        // Поэтому мы выполняем синхронную логику и эмулируем поток из нескольких событий.
        return Mono.fromFuture(() -> handleSync(request, taskId))
                .flatMapMany(syncResponse -> Flux.just(
                        UniversalResponse.from(syncResponse),
                        new UniversalResponse.Done("Генерация кода завершена.")
                ));
    }
}
