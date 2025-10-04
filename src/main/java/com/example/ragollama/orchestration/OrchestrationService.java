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
 * Сервис-оркестратор, который является единой точкой входа для обработки
 * всех пользовательских запросов.
 * <p>
 * Его обязанности:
 * <ol>
 *     <li>Создать и зарегистрировать асинхронную задачу через {@link TaskLifecycleService}.</li>
 *     <li>Определить намерение (intent) пользователя с помощью {@link RouterAgentService}.</li>
 *     <li>Выбрать и делегировать выполнение соответствующему {@link IntentHandler}.</li>
 *     <li>Обеспечить корректную обработку успешного завершения, ошибок и отмены задачи.</li>
 * </ol>
 * Эта версия адаптирована для работы в полностью реактивной среде,
 * корректно обрабатывая исключения, связанные с разрывом соединения клиентом.
 */
@Slf4j
@Service
public class OrchestrationService {

    private final RouterAgentService router;
    private final Map<QueryIntent, IntentHandler> handlerMap;
    private final TaskLifecycleService taskService;

    /**
     * Конструктор, который автоматически обнаруживает все реализации {@link IntentHandler}
     * и регистрирует их в карте для быстрой маршрутизации.
     *
     * @param handlers  Список всех бинов-обработчиков.
     * @param router    Сервис-маршрутизатор.
     * @param taskService Сервис управления задачами.
     */
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

    /**
     * Логирует информацию о зарегистрированных обработчиках при старте приложения.
     */
    @PostConstruct
    public void init() {
        log.info("OrchestrationService инициализирован. Зарегистрировано {} обработчиков для интентов: {}", handlerMap.size(), handlerMap.keySet());
    }

    /**
     * Асинхронно обрабатывает запрос и возвращает ID задачи для отслеживания.
     *
     * @param request Универсальный запрос от пользователя.
     * @return DTO с ID созданной задачи.
     */
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


    /**
     * Обрабатывает запрос в потоковом режиме (SSE).
     *
     * @param request Универсальный запрос от пользователя.
     * @return Реактивный поток {@link UniversalResponse}.
     */
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
                    if (cause instanceof CancellationException || cause instanceof IOException) {
                        log.warn("Соединение было разорвано клиентом. Чисто завершаем поток.");
                        return Flux.empty();
                    }
                    log.error("Непредвиденная ошибка в потоке: {}", e.getMessage(), cause);
                    return Flux.just(new UniversalResponse.Error("Произошла внутренняя ошибка: " + e.getMessage()));
                });
    }

    /**
     * Находит подходящий обработчик для заданного намерения.
     *
     * @param intent Намерение пользователя.
     * @return Найденный {@link IntentHandler}.
     * @throws IllegalStateException если обработчик не найден.
     */
    private IntentHandler findHandler(QueryIntent intent) {
        IntentHandler handler = handlerMap.get(intent);
        if (handler == null) {
            log.error("Для намерения '{}' не найден соответствующий обработчик.", intent);
            throw new IllegalStateException("Нет обработчика для намерения: " + intent);
        }
        return handler;
    }
}
