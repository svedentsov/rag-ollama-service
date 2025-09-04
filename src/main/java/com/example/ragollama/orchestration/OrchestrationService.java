package com.example.ragollama.orchestration;

import com.example.ragollama.agent.buganalysis.domain.BugAnalysisService;
import com.example.ragollama.agent.codegeneration.domain.CodeGenerationService;
import com.example.ragollama.agent.routing.RouterAgentService;
import com.example.ragollama.optimization.AdaptiveRagOrchestrator;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.dto.UniversalSyncResponse;
import com.example.ragollama.summarization.SummarizationService;
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
    private final ChatApplicationService chatApplicationService;
    private final CodeGenerationService codeGenerationService;
    private final BugAnalysisService bugAnalysisService;
    private final SummarizationService summarizationService;
    private final AdaptiveRagOrchestrator adaptiveRagOrchestrator; // <-- ЗАВИСИМОСТЬ

    /**
     * Обрабатывает унифицированный запрос от пользователя, возвращая полный ответ после его генерации.
     *
     * @param request DTO с запросом пользователя.
     * @return {@link CompletableFuture} с полным, агрегированным ответом.
     */
    public CompletableFuture<UniversalSyncResponse> processSync(UniversalRequest request) {
        return router.route(request.query())
                .flatMap(intent -> {
                    log.info("Маршрутизация запроса с намерением: {}. SessionID: {}", intent, request.sessionId());
                    return switch (intent) {
                        case RAG_QUERY ->
                            // ИСПОЛЬЗУЕМ АДАПТИВНЫЙ ОРКЕСТРАТОР
                                Mono.fromFuture(() -> adaptiveRagOrchestrator.processAdaptive(request.toRagQueryRequest()))
                                        .map(response -> UniversalSyncResponse.from(response, intent));
                        case CHITCHAT, UNKNOWN ->
                                Mono.fromFuture(() -> chatApplicationService.processChatRequestAsync(request.toChatRequest()))
                                        .map(response -> UniversalSyncResponse.from(response, intent));
                        case CODE_GENERATION -> codeGenerationService.generateCode(request.toCodeGenerationRequest())
                                .map(response -> UniversalSyncResponse.from(response, intent));
                        case BUG_ANALYSIS -> bugAnalysisService.analyzeBugReport(request.query())
                                .map(response -> UniversalSyncResponse.from(response, intent));
                        case SUMMARIZATION -> summarizationService.summarizeAsync(request.context(), null)
                                .map(summary -> UniversalSyncResponse.from(summary, intent));
                    };
                }).toFuture();
    }

    /**
     * Обрабатывает универсальный потоковый запрос.
     *
     * @param request DTO с запросом пользователя.
     * @return {@link Flux} со структурированными частями ответа.
     */
    public Flux<UniversalResponse> processStream(UniversalRequest request) {
        return router.route(request.query())
                .flatMapMany(intent -> {
                    log.info("Маршрутизация потокового запроса с намерением: {}. SessionID: {}", intent, request.sessionId());
                    return switch (intent) {
                        case RAG_QUERY ->
                                chatApplicationService.processChatRequestStream(request.toChatRequest())
                                        .map(UniversalResponse::from);
                        case CHITCHAT, UNKNOWN ->
                                chatApplicationService.processChatRequestStream(request.toChatRequest())
                                        .map(UniversalResponse::from);
                        case CODE_GENERATION -> chatApplicationService.processChatRequestStream(request.toChatRequest())
                                .map(UniversalResponse::from);
                        case BUG_ANALYSIS -> bugAnalysisService.analyzeBugReport(request.query())
                                .map(UniversalResponse::from)
                                .flux();
                        case SUMMARIZATION -> summarizationService.summarizeAsync(request.context(), null)
                                .map(UniversalResponse::from)
                                .flux();
                    };
                });
    }
}
