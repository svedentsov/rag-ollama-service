package com.example.ragollama.orchestration;

import com.example.ragollama.agent.domain.CodeGenerationService;
import com.example.ragollama.agent.domain.RouterAgentService;
import com.example.ragollama.chat.domain.ChatService;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.dto.UniversalSyncResponse;
import com.example.ragollama.rag.domain.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Сервис-оркестратор, который является единой точкой входа для всех запросов.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestrationService {

    private final RouterAgentService router;
    private final RagService ragService;
    private final ChatService chatService;
    private final CodeGenerationService codeGenerationService;

    /**
     * Обрабатывает унифицированный запрос от пользователя, возвращая полный ответ после его генерации.
     *
     * @param request DTO с запросом пользователя.
     * @return {@link CompletableFuture} с полным, агрегированным ответом.
     */
    public CompletableFuture<UniversalSyncResponse> processSync(UniversalRequest request) {
        return router.route(request.query()).toFuture()
                .thenCompose(intent -> {
                    log.info("Маршрутизация запроса с намерением: {}. SessionID: {}", intent, request.sessionId());
                    return switch (intent) {
                        case RAG_QUERY -> ragService.queryAsync(request.toRagQueryRequest())
                                .thenApply(response -> UniversalSyncResponse.from(response, intent));
                        case CHITCHAT -> chatService.processChatRequestAsync(request.toChatRequest())
                                .thenApply(response -> UniversalSyncResponse.from(response, intent));
                        case CODE_GENERATION, UNKNOWN ->
                                codeGenerationService.generateCode(request.toCodeGenerationRequest())
                                        .thenApply(response -> UniversalSyncResponse.from(response, intent));
                    };
                });
    }

    /**
     * Обрабатывает универсальный потоковый запрос.
     */
    public Flux<UniversalResponse> processStream(UniversalRequest request) {
        return router.route(request.query())
                .flatMapMany(intent -> {
                    log.info("Маршрутизация потокового запроса с намерением: {}. SessionID: {}", intent, request.sessionId());
                    return switch (intent) {
                        case RAG_QUERY -> ragService.queryStream(request.toRagQueryRequest())
                                .map(UniversalResponse::from);
                        case CHITCHAT -> chatService.processChatRequestStream(request.toChatRequest())
                                .map(UniversalResponse::from);
                        case CODE_GENERATION, UNKNOWN ->
                                Mono.fromFuture(() -> codeGenerationService.generateCode(request.toCodeGenerationRequest()))
                                        .map(UniversalResponse::from)
                                        .flux();
                    };
                });
    }
}
