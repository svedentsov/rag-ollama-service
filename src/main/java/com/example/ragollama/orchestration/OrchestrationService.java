package com.example.ragollama.orchestration;

import com.example.ragollama.agent.buganalysis.domain.BugAnalysisService;
import com.example.ragollama.agent.codegeneration.domain.CodeGenerationService;
import com.example.ragollama.agent.routing.RouterAgentService;
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
 * <p>
 * Эта финальная версия вводит жесткие бизнес-правила по использованию полей
 * из {@link UniversalRequest} в зависимости от определенного намерения.
 * Все асинхронные операции теперь унифицированы с использованием Project Reactor.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestrationService {

    private final RouterAgentService router;
    private final ChatSessionService sessionService;
    private final CodeGenerationService codeGenerationService;
    private final BugAnalysisService bugAnalysisService;
    private final SummarizationService summarizationService;

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
                        // Для всех интентов, кроме SUMMARIZATION, используем поле `query`
                        case RAG_QUERY ->
                                Mono.fromFuture(sessionService.processRagRequestAsync(request.toRagQueryRequest()))
                                        .map(response -> UniversalSyncResponse.from(response, intent));
                        case CHITCHAT ->
                                Mono.fromFuture(sessionService.processChatRequestAsync(request.toChatRequest()))
                                        .map(response -> UniversalSyncResponse.from(response, intent));
                        case CODE_GENERATION -> codeGenerationService.generateCode(request.toCodeGenerationRequest())
                                .map(response -> UniversalSyncResponse.from(response, intent));
                        case BUG_ANALYSIS -> bugAnalysisService.analyzeBugReport(request.query())
                                .map(response -> UniversalSyncResponse.from(response, intent));
                        // Для SUMMARIZATION используем ТОЛЬКО поле `context`
                        case SUMMARIZATION -> summarizationService.summarizeAsync(request.context(), null)
                                .map(summary -> UniversalSyncResponse.from(summary, intent));
                        case UNKNOWN -> // Fallback-поведение
                                Mono.fromFuture(sessionService.processChatRequestAsync(request.toChatRequest()))
                                        .map(response -> UniversalSyncResponse.from(response, intent));
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
                        case RAG_QUERY -> sessionService.processRagRequestStream(request.toRagQueryRequest())
                                .map(UniversalResponse::from);
                        case CHITCHAT -> sessionService.processChatRequestStream(request.toChatRequest())
                                .map(UniversalResponse::from);
                        case CODE_GENERATION ->
                                Flux.from(sessionService.processChatRequestStream(request.toChatRequest()))
                                        .map(UniversalResponse::from);
                        case BUG_ANALYSIS -> bugAnalysisService.analyzeBugReport(request.query())
                                .map(UniversalResponse::from)
                                .flux();
                        case SUMMARIZATION -> summarizationService.summarizeAsync(request.context(), null)
                                .map(UniversalResponse::from)
                                .flux();
                        case UNKNOWN -> Flux.from(sessionService.processChatRequestStream(request.toChatRequest()))
                                .map(UniversalResponse::from);
                    };
                });
    }
}
