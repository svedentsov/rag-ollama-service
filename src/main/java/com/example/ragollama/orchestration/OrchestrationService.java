package com.example.ragollama.orchestration;

import com.example.ragollama.agent.routing.QueryIntent;
import com.example.ragollama.agent.routing.RouterAgentService;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.handlers.IntentHandler;
import com.example.ragollama.shared.task.CancellableTaskService;
import com.example.ragollama.shared.task.TaskStateService;
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

/**
 * Главный сервис-оркестратор, который является единой точкой входа для
 * обработки всех пользовательских запросов.
 * <p>
 * Эта версия интегрирована с новой системой управления задачами
 * ({@link CancellableTaskService} и {@link TaskStateService}), обеспечивая
 * полный контроль над жизненным циклом асинхронных операций.
 */
@Slf4j
@Service
public class OrchestrationService {

    private final RouterAgentService router;
    private final Map<QueryIntent, IntentHandler> handlerMap;
    private final CancellableTaskService taskService;
    private final TaskStateService taskStateService;

    /**
     * Конструктор, который автоматически обнаруживает все реализации
     * {@link IntentHandler} и строит из них карту-маршрутизатор.
     *
     * @param handlers         Список всех бинов-обработчиков.
     * @param router           Сервис для определения намерения.
     * @param taskService      Сервис для управления задачами.
     * @param taskStateService Сервис для связи сессий и задач.
     */
    @Autowired
    public OrchestrationService(List<IntentHandler> handlers, RouterAgentService router, CancellableTaskService taskService, TaskStateService taskStateService) {
        this.router = router;
        this.taskService = taskService;
        this.taskStateService = taskStateService;
        this.handlerMap = new EnumMap<>(QueryIntent.class);
        for (IntentHandler handler : handlers) {
            handlerMap.put(handler.canHandle(), handler);
            if (handler.fallbackIntent() != null) {
                handlerMap.put(handler.fallbackIntent(), handler);
            }
        }
    }

    /**
     * Логирует информацию о зарегистрированных обработчиках после инициализации бина.
     */
    @PostConstruct
    public void init() {
        log.info("OrchestrationService инициализирован. Зарегистрировано {} обработчиков для интентов: {}",
                handlerMap.size(), handlerMap.keySet());
    }

    /**
     * Асинхронно обрабатывает запрос, регистрируя его как управляемую задачу.
     *
     * @param request Универсальный запрос.
     * @return DTO с ID созданной задачи.
     */
    public TaskSubmissionResponse processAsync(UniversalRequest request) {
        CompletableFuture<?> taskFuture = router.route(request.query())
                .flatMap(intent -> {
                    log.info("Маршрутизация запроса с намерением: {}. SessionID: {}", intent, request.sessionId());
                    IntentHandler handler = findHandler(intent);
                    return Mono.fromFuture(handler.handleSync(request));
                }).toFuture();

        UUID taskId = taskService.register(taskFuture);
        if (request.sessionId() != null) {
            taskStateService.registerSessionTask(request.sessionId(), taskId);
        }
        taskFuture.whenComplete((res, err) -> {
            if (request.sessionId() != null) {
                taskStateService.clearSessionTask(request.sessionId());
            }
        });
        return new TaskSubmissionResponse(taskId);
    }

    /**
     * Обрабатывает запрос в потоковом режиме, регистрируя его как управляемую задачу.
     *
     * @param request Универсальный запрос.
     * @return Поток событий, начинающийся с ID задачи.
     */
    public Flux<UniversalResponse> processStream(UniversalRequest request) {
        return router.route(request.query())
                .flatMapMany(intent -> {
                    CompletableFuture<Void> streamCompletionFuture = new CompletableFuture<>();
                    UUID taskId = taskService.register(streamCompletionFuture);
                    if (request.sessionId() != null) {
                        taskStateService.registerSessionTask(request.sessionId(), taskId);
                    }
                    log.info("Маршрутизация потокового запроса с намерением: {}. TaskID: {}", intent, taskId);

                    IntentHandler handler = findHandler(intent);

                    Flux<UniversalResponse> responseStream = handler.handleStream(request);

                    responseStream
                            .doOnTerminate(() -> {
                                if (!streamCompletionFuture.isDone()) {
                                    streamCompletionFuture.complete(null);
                                }
                            })
                            .doOnError(streamCompletionFuture::completeExceptionally)
                            .subscribe(
                                    event -> taskService.emitEvent(taskId, event),
                                    error -> {
                                    },
                                    () -> {
                                    }
                            );

                    return Flux.concat(
                            Flux.just(new UniversalResponse.TaskStarted(taskId)),
                            taskService.getTaskStream(taskId).orElse(Flux.empty())
                    );
                })
                .doOnTerminate(() -> {
                    if (request.sessionId() != null) {
                        taskStateService.clearSessionTask(request.sessionId());
                    }
                })
                .onErrorResume(e -> {
                    Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                    if (cause instanceof CancellationException || cause instanceof IOException) {
                        log.warn("Соединение было разорвано клиентом (IOException/Cancellation). Чисто завершаем поток.");
                        return Flux.empty();
                    }
                    log.error("Непредвиденная ошибка в потоке: {}", e.getMessage(), cause);
                    return Flux.just(new UniversalResponse.Error("Произошла внутренняя ошибка: " + e.getMessage()));
                });
    }

    /**
     * Находит подходящий обработчик для заданного намерения.
     *
     * @param intent Намерение.
     * @return Найденный {@link IntentHandler}.
     * @throws IllegalStateException если обработчик не найден.
     */
    private IntentHandler findHandler(QueryIntent intent) {
        IntentHandler handler = handlerMap.get(intent);
        if (handler == null) {
            log.error("Для намерения '{}' не найден соответствующий обработчик. Проверьте конфигурацию.", intent);
            throw new IllegalStateException("Нет обработчика для намерения: " + intent);
        }
        return handler;
    }
}
