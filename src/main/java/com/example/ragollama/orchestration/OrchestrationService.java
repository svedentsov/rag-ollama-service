package com.example.ragollama.orchestration;

import com.example.ragollama.agent.routing.QueryIntent;
import com.example.ragollama.agent.routing.RouterAgentService;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.handlers.IntentHandler;
import com.example.ragollama.shared.task.CancellableTaskService;
import com.example.ragollama.shared.task.TaskSubmissionResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
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
    private final CancellableTaskService taskService;

    @Autowired
    public OrchestrationService(List<IntentHandler> handlers, RouterAgentService router, CancellableTaskService taskService) {
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
        log.info("OrchestrationService инициализирован. Зарегистрировано {} обработчиков для интентов: {}",
                handlerMap.size(), handlerMap.keySet());
    }

    public TaskSubmissionResponse processAsync(UniversalRequest request) {
        CompletableFuture<?> taskFuture = router.route(request.query())
                .flatMap(intent -> {
                    log.info("Маршрутизация запроса с намерением: {}. SessionID: {}", intent, request.sessionId());
                    IntentHandler handler = findHandler(intent);
                    return Mono.fromFuture(handler.handleSync(request));
                }).toFuture();

        UUID taskId = taskService.register(taskFuture);
        return new TaskSubmissionResponse(taskId);
    }

    public Flux<UniversalResponse> processStream(UniversalRequest request) {
        CompletableFuture<Flux<UniversalResponse>> fluxFuture = new CompletableFuture<>();
        UUID taskId = taskService.register(fluxFuture);

        router.route(request.query())
                .doOnSuccess(intent -> {
                    log.info("Маршрутизация потокового запроса с намерением: {}. SessionID: {}", intent, request.sessionId());
                    IntentHandler handler = findHandler(intent);
                    Flux<UniversalResponse> responseFlux = handler.handleStream(request)
                            .doOnCancel(() -> {
                                log.warn("Поток для задачи {} был отменен клиентом (doOnCancel).", taskId);
                                taskService.cancel(taskId);
                            });
                    fluxFuture.complete(responseFlux);
                })
                .doOnError(fluxFuture::completeExceptionally)
                .subscribe();

        UniversalResponse.TaskStarted taskStartedEvent = new UniversalResponse.TaskStarted(taskId);

        return Flux.concat(
                Flux.just(taskStartedEvent),
                Flux.from(Mono.fromFuture(fluxFuture)).flatMap(flux -> flux)
        ).onErrorResume(e -> { // !!! КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ !!!
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;

            if (cause instanceof CancellationException) {
                log.warn("Задача {} была отменена, чисто завершаем поток SSE.", taskId);
                return Flux.empty();
            }
            if (cause instanceof IOException) {
                log.warn("Соединение для задачи {} было разорвано клиентом (IOException). Чисто завершаем поток.", taskId);
                return Flux.empty();
            }

            log.error("Непредвиденная ошибка в основном потоке задачи {}: {}", taskId, e.getMessage(), cause);
            return Flux.just(new UniversalResponse.Error("Произошла внутренняя ошибка: " + e.getMessage()));
        });
    }


    private IntentHandler findHandler(QueryIntent intent) {
        IntentHandler handler = handlerMap.get(intent);
        if (handler == null) {
            log.error("Для намерения '{}' не найден соответствующий обработчик. Проверьте конфигурацию.", intent);
            throw new IllegalStateException("Нет обработчика для намерения: " + intent);
        }
        return handler;
    }
}
