package com.example.ragollama.orchestration;

import com.example.ragollama.agent.routing.QueryIntent;
import com.example.ragollama.agent.routing.RouterAgentService;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.orchestration.dto.UniversalSyncResponse;
import com.example.ragollama.orchestration.handlers.IntentHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-оркестратор, который является единой точкой входа для всех запросов.
 * <p>
 * Эта версия реализует паттерн "Стратегия". Она больше не содержит `switch`-конструкцию,
 * а делегирует выполнение конкретному обработчику {@link IntentHandler},
 * который выбирается на основе результата работы {@link RouterAgentService}.
 * Контракт {@link IntentHandler} был разделен на два метода для явной
 * поддержки синхронных и потоковых ответов.
 */
@Slf4j
@Service
public class OrchestrationService {

    private final RouterAgentService router;
    private final Map<QueryIntent, IntentHandler> handlerMap;

    /**
     * Конструктор, который автоматически обнаруживает все реализации {@link IntentHandler}
     * и индексирует их в {@link EnumMap} для сверхбыстрого доступа.
     * <p>
     * Аннотация {@code @Autowired} явно указывает Spring использовать этот конструктор
     * для внедрения зависимостей, что решает проблему {@code UnsatisfiedDependencyException}.
     *
     * @param handlers Список всех бинов {@link IntentHandler}, предоставленный Spring.
     * @param router   Сервис для определения намерения.
     */
    @Autowired
    public OrchestrationService(List<IntentHandler> handlers, RouterAgentService router) {
        this.router = router;
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
        log.info("OrchestrationService инициализирован. Зарегистрировано {} обработчиков для интентов: {}",
                handlerMap.size(), handlerMap.keySet());
    }

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
                    IntentHandler handler = findHandler(intent);
                    // Вызываем синхронный метод обработчика и оборачиваем его в Mono
                    return Mono.fromFuture(handler.handleSync(request));
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
                    IntentHandler handler = findHandler(intent);
                    // Вызываем потоковый метод обработчика
                    return handler.handleStream(request);
                });
    }

    /**
     * Находит подходящий обработчик для заданного намерения.
     *
     * @param intent Намерение пользователя.
     * @return Найденный {@link IntentHandler}.
     * @throws IllegalStateException если подходящий обработчик не зарегистрирован.
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
