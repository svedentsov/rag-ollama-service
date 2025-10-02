package com.example.ragollama.orchestration;

import com.example.ragollama.agent.routing.QueryIntent;
import com.example.ragollama.agent.routing.RouterAgentService;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.dto.UniversalSyncResponse;
import com.example.ragollama.orchestration.handlers.IntentHandler;
import com.example.ragollama.shared.task.TaskLifecycleService;
import com.example.ragollama.shared.task.TaskSubmissionResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class OrchestrationService {

    private final RouterAgentService router;
    private final Map<QueryIntent, IntentHandler> handlerMap;
    private final TaskLifecycleService taskService;

    @Autowired
    public OrchestrationService(List<IntentHandler> handlers, RouterAgentService router, TaskLifecycleService taskService) {
        this.router = router;
        this.taskService = taskService;
        this.handlerMap = new EnumMap<>(QueryIntent.class);
        for (IntentHandler handler : handlers) {
            handlerMap.put(handler.canHandle(), handler);
            if (handler.fallbackIntent() != null) {
                handlerMap.put(handler.fallbackIntent(), handler);
            }
        }
    }

    @PostConstruct
    public void init() {
        log.info("OrchestrationService инициализирован. Зарегистрировано {} обработчиков для интентов: {}", handlerMap.size(), handlerMap.keySet());
    }

    public TaskSubmissionResponse processAsync(UniversalRequest request) {
        CompletableFuture<UniversalSyncResponse> taskFuture = new CompletableFuture<>();
        UUID taskId = taskService.register(taskFuture, request.sessionId());

        router.route(request.query())
                .flatMap(intent -> {
                    log.info("Маршрутизация запроса с намерением: {}. TaskID: {}", intent, taskId);
                    IntentHandler handler = findHandler(intent);
                    return Mono.fromFuture(handler.handleSync(request, taskId));
                })
                .doOnSuccess(taskFuture::complete)
                .doOnError(taskFuture::completeExceptionally)
                .subscribe();

        return new TaskSubmissionResponse(taskId);
    }


    public Flux<UniversalResponse> processStream(UniversalRequest request) {
        return router.route(request.query())
                .flatMapMany(intent -> {
                    CompletableFuture<Void> streamCompletionFuture = new CompletableFuture<>();
                    UUID taskId = taskService.register(streamCompletionFuture, request.sessionId());
                    log.info("Маршрутизация потокового запроса с намерением: {}. TaskID: {}", intent, taskId);

                    IntentHandler handler = findHandler(intent);
                    Flux<UniversalResponse> responseStream = handler.handleStream(request, taskId);

                    responseStream
                            .doOnTerminate(() -> {
                                if (!streamCompletionFuture.isDone()) streamCompletionFuture.complete(null);
                            })
                            .doOnError(streamCompletionFuture::completeExceptionally)
                            .subscribe(
                                    event -> taskService.emitEvent(taskId, event),
                                    error -> {},
                                    () -> {}
                            );

                    return Flux.concat(
                            Flux.just(new UniversalResponse.TaskStarted(taskId)),
                            taskService.getTaskStream(taskId).orElse(Flux.empty())
                    );
                })
                .onErrorResume(e -> {
                    Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                    if (cause instanceof ClientAbortException || cause instanceof CancellationException) {
                        log.warn("Соединение было разорвано клиентом. Чисто завершаем поток.");
                        return Flux.empty();
                    }
                    log.error("Непредвиденная ошибка в потоке: {}", e.getMessage(), cause);
                    return Flux.just(new UniversalResponse.Error("Произошла внутренняя ошибка: " + e.getMessage()));
                });
    }

    private IntentHandler findHandler(QueryIntent intent) {
        IntentHandler handler = handlerMap.get(intent);
        if (handler == null) {
            log.error("Для намерения '{}' не найден соответствующий обработчик.", intent);
            throw new IllegalStateException("Нет обработчика для намерения: " + intent);
        }
        return handler;
    }
}
