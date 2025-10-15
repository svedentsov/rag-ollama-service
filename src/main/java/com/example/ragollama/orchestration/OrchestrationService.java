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
 * Сервис-оркестратор, адаптированный для работы с реактивными сервисами.
 * Отвечает за маршрутизацию входящих запросов к соответствующим обработчикам
 * на основе определенного намерения (intent).
 */
@Slf4j
@Service
public class OrchestrationService {

    private final RouterAgentService router;
    private final Map<QueryIntent, IntentHandler> handlerMap;
    private final TaskLifecycleService taskService;

    /**
     * Конструктор для внедрения зависимостей. Автоматически обнаруживает
     * все бины, реализующие {@link IntentHandler}, и регистрирует их.
     *
     * @param handlers    Список всех доступных обработчиков намерений.
     * @param router      Сервис для определения намерения пользователя.
     * @param taskService Сервис для управления жизненным циклом асинхронных задач.
     */
    @Autowired
    public OrchestrationService(List<IntentHandler> handlers, RouterAgentService router, TaskLifecycleService taskService) {
        this.router = router;
        this.taskService = taskService;
        this.handlerMap = new EnumMap<>(QueryIntent.class);
        handlers.forEach(handler -> {
            handlerMap.put(handler.canHandle(), handler);
            if (handler.fallbackIntent() != null) {
                handlerMap.put(handler.fallbackIntent(), handler);
            }
        });
    }

    /**
     * Инициализирует сервис и логирует зарегистрированные обработчики.
     */
    @PostConstruct
    public void init() {
        log.info("OrchestrationService: {} обработчиков зарегистрировано для интентов: {}", handlerMap.size(), handlerMap.keySet());
    }

    /**
     * Асинхронно запускает выполнение задачи и немедленно возвращает ее ID.
     *
     * @param request Унифицированный DTO с запросом от пользователя.
     * @return Объект с ID задачи для отслеживания.
     */
    public TaskSubmissionResponse processAsync(UniversalRequest request) {
        CompletableFuture<UniversalSyncResponse> taskFuture = new CompletableFuture<>();
        UUID taskId = taskService.register(taskFuture, request.sessionId()).block();

        router.route(request.query(), request.context())
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
     * Обрабатывает любой пользовательский запрос в потоковом режиме (Server-Sent Events).
     *
     * @param request Унифицированный DTO с запросом от пользователя.
     * @return Реактивный поток {@link UniversalResponse}.
     */
    public Flux<UniversalResponse> processStream(UniversalRequest request) {
        return router.route(request.query(), request.context())
                .flatMapMany(intent ->
                        taskService.register(new CompletableFuture<Void>(), request.sessionId())
                                .flatMapMany(taskId -> {
                                    log.info("Маршрутизация потокового запроса: {}. TaskID: {}", intent, taskId);

                                    IntentHandler handler = findHandler(intent);
                                    Flux<UniversalResponse> responseStream = handler.handleStream(request, taskId);

                                    return Flux.concat(
                                                    Flux.just(new UniversalResponse.TaskStarted(taskId)),
                                                    responseStream
                                            ).doOnTerminate(() -> taskService.updateTaskStatusOnCompletion(taskId, null).subscribe())
                                            .doOnError(e -> taskService.updateTaskStatusOnCompletion(taskId, e).subscribe());
                                })
                )
                .onErrorResume(e -> {
                    Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                    if (cause instanceof CancellationException || cause instanceof IOException) {
                        log.warn("Соединение разорвано клиентом: {}", cause.getMessage());
                        return Flux.empty();
                    }
                    log.error("Непредвиденная ошибка в потоке: {}", e.getMessage(), cause);
                    return Flux.just(new UniversalResponse.Error("Внутренняя ошибка: " + e.getMessage()));
                });
    }

    /**
     * Находит подходящий обработчик для заданного намерения.
     *
     * @param intent Намерение, для которого нужен обработчик.
     * @return Найденный {@link IntentHandler}.
     * @throws IllegalStateException если обработчик не найден.
     */
    private IntentHandler findHandler(QueryIntent intent) {
        IntentHandler handler = handlerMap.get(intent);
        if (handler == null) {
            log.error("Для намерения '{}' не найден обработчик.", intent);
            throw new IllegalStateException("Нет обработчика для: " + intent);
        }
        return handler;
    }
}
