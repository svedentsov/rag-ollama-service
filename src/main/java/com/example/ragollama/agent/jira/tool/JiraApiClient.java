package com.example.ragollama.agent.jira.tool;

import com.example.ragollama.agent.config.JiraProperties;
import com.example.ragollama.agent.jira.tool.dto.JiraIssueDto;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Клиент для взаимодействия с Jira Cloud REST API.
 * <p>
 * Инкапсулирует логику HTTP-запросов к Jira, используя неблокирующий
 * {@link WebClient}. Все публичные методы защищены механизмами
 * отказоустойчивости Resilience4j (Retry, Circuit Breaker, Timeout).
 */
@Slf4j
@Service
public class JiraApiClient {

    private static final String JIRA_RESILIENCE_CONFIG = "jira";
    private final WebClient webClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    private CircuitBreaker circuitBreaker;
    private Retry retry;
    private TimeLimiter timeLimiter;

    /**
     * Конструктор, который создает и настраивает {@link WebClient} для Jira.
     *
     * @param webClientBuilder       Строитель {@link WebClient} из общей конфигурации.
     * @param properties             Типобезопасная конфигурация для Jira.
     * @param circuitBreakerRegistry Реестр Circuit Breaker'ов.
     * @param retryRegistry          Реестр Retry.
     * @param timeLimiterRegistry    Реестр Time Limiter'ов.
     */
    public JiraApiClient(WebClient.Builder webClientBuilder,
                         JiraProperties properties, // Заменяем множество @Value на один объект
                         CircuitBreakerRegistry circuitBreakerRegistry,
                         RetryRegistry retryRegistry,
                         TimeLimiterRegistry timeLimiterRegistry) {
        this.webClient = webClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeaders(h -> h.setBasicAuth(properties.apiUser(), properties.apiToken()))
                .build();
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.timeLimiterRegistry = timeLimiterRegistry;
    }

    /**
     * Инициализирует компоненты Resilience4j после создания бина.
     */
    @PostConstruct
    public void init() {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(JIRA_RESILIENCE_CONFIG);
        this.retry = retryRegistry.retry(JIRA_RESILIENCE_CONFIG);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(JIRA_RESILIENCE_CONFIG);
    }

    /**
     * Асинхронно извлекает детали задачи из Jira.
     *
     * @param issueKey Ключ задачи (например, "PROJ-123").
     * @return {@link Mono} с DTO, содержащим информацию о задаче.
     */
    public Mono<JiraIssueDto> getIssueDetails(String issueKey) {
        log.debug("Запрос деталей для задачи Jira: {}", issueKey);
        Mono<JiraIssueDto> requestMono = webClient.get()
                .uri("/rest/api/3/issue/{issueKey}", issueKey)
                .retrieve()
                .bodyToMono(JiraIssueDto.class);

        return applyResilience(requestMono)
                .doOnError(e -> log.error("Ошибка при запросе деталей задачи {}: {}", issueKey, e.getMessage()));
    }

    /**
     * Асинхронно публикует комментарий к задаче в Jira.
     *
     * @param issueKey Ключ задачи (например, "PROJ-123").
     * @param comment  Текст комментария.
     * @return {@link Mono}, который завершается при успешной публикации.
     */
    public Mono<Void> postCommentToIssue(String issueKey, String comment) {
        log.info("Публикация комментария в задачу Jira: {}", issueKey);
        Map<String, Object> body = Map.of(
                "body", Map.of(
                        "type", "doc",
                        "version", 1,
                        "content", List.of(
                                Map.of(
                                        "type", "paragraph",
                                        "content", List.of(
                                                Map.of("type", "text", "text", comment)
                                        )
                                )
                        )
                )
        );

        Mono<Void> requestMono = webClient.post()
                .uri("/rest/api/3/issue/{issueKey}/comment", issueKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class);

        return applyResilience(requestMono)
                .doOnSuccess(v -> log.info("Комментарий успешно опубликован в задаче {}", issueKey))
                .doOnError(e -> log.error("Ошибка при публикации комментария в задачу {}: {}", issueKey, e.getMessage()));
    }

    /**
     * Применяет общую цепочку отказоустойчивости к Mono-паблишеру.
     *
     * @param publisher Исходный {@link Mono} с запросом.
     * @param <T>       Тип ответа.
     * @return {@link Mono}, обернутый в Retry, TimeLimiter и CircuitBreaker.
     */
    private <T> Mono<T> applyResilience(Mono<T> publisher) {
        return publisher
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
    }
}
