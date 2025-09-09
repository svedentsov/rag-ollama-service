package com.example.ragollama.orchestration;

import com.example.ragollama.agent.codegeneration.domain.CodeGenerationService;
import com.example.ragollama.agent.routing.QueryIntent;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.dto.UniversalSyncResponse;
import com.example.ragollama.orchestration.handlers.IntentHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * Обработчик, реализующий логику для намерения {@link QueryIntent#CODE_GENERATION}.
 * <p>
 * Этот компонент является частью паттерна "Стратегия". Он инкапсулирует
 * вызов {@link CodeGenerationService} и преобразование его результата в
 * унифицированный формат ответа.
 */
@Service
@RequiredArgsConstructor
public class CodeGenerationIntentHandler implements IntentHandler {

    private final CodeGenerationService codeGenerationService;

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
        return codeGenerationService.generateCode(request.toCodeGenerationRequest())
                .map(response -> UniversalSyncResponse.from(response, canHandle()))
                .toFuture();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Flux<UniversalResponse> handleStream(UniversalRequest request) {
        return codeGenerationService.generateCode(request.toCodeGenerationRequest())
                .map(UniversalResponse::from)
                .flux();
    }
}
