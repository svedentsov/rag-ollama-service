package com.example.ragollama.crawler.confluence.tool;

import com.example.ragollama.crawler.confluence.ConfluenceProperties;
import com.example.ragollama.crawler.confluence.tool.dto.ConfluencePageDto;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Клиент для взаимодействия с Confluence Cloud REST API.
 * <p>
 * Инкапсулирует логику HTTP-запросов к Confluence, используя неблокирующий
 * {@link WebClient}. Все публичные методы защищены механизмами
 * отказоустойчивости Resilience4j.
 */
@Slf4j
@Service
public class ConfluenceApiClient {

    private static final String CONFLUENCE_RESILIENCE_CONFIG = "confluence";
    private final WebClient webClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    private CircuitBreaker circuitBreaker;
    private Retry retry;
    private TimeLimiter timeLimiter;

    /**
     * Конструктор, который создает и настраивает {@link WebClient} для Confluence.
     *
     * @param webClientBuilder       Строитель {@link WebClient} из общей конфигурации.
     * @param properties             Типобезопасная конфигурация для Confluence.
     * @param circuitBreakerRegistry Реестр Circuit Breaker'ов.
     * @param retryRegistry          Реестр Retry.
     * @param timeLimiterRegistry    Реестр Time Limiter'ов.
     */
    public ConfluenceApiClient(WebClient.Builder webClientBuilder,
                               ConfluenceProperties properties,
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
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CONFLUENCE_RESILIENCE_CONFIG);
        this.retry = retryRegistry.retry(CONFLUENCE_RESILIENCE_CONFIG);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(CONFLUENCE_RESILIENCE_CONFIG);
    }

    /**
     * Рекурсивно извлекает все страницы в указанном пространстве.
     * <p>
     * Обрабатывает пагинацию Confluence API, автоматически запрашивая
     * следующие страницы, пока они доступны.
     *
     * @param spaceKey Ключ пространства Confluence.
     * @return {@link Flux}, который эмитит DTO для каждой найденной страницы.
     */
    public Flux<ConfluencePageDto> fetchAllPagesInSpace(String spaceKey) {
        return fetchPaginatedPages("/rest/api/space/" + spaceKey + "/content/page");
    }

    /**
     * Получает полный контент страницы, включая ее тело и информацию о версии.
     *
     * @param pageId ID страницы в Confluence.
     * @return {@link Mono} с DTO страницы, содержащим ее контент.
     */
    public Mono<ConfluencePageDto> getPageContent(String pageId) {
        log.trace("Запрос контента для страницы ID: {}", pageId);
        Mono<ConfluencePageDto> requestMono = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/api/page/{id}")
                        .queryParam("expand", "body.storage,space,version")
                        .build(pageId))
                .retrieve()
                .bodyToMono(ConfluencePageDto.class);

        return applyResilience(requestMono)
                .doOnError(e -> log.error("Ошибка при запросе контента страницы {}: {}", pageId, e.getMessage()));
    }

    /**
     * Вспомогательный метод для обработки пагинированных ответов от API.
     *
     * @param initialUri URI для первого запроса.
     * @return {@link Flux} со всеми результатами со всех страниц.
     */
    private Flux<ConfluencePageDto> fetchPaginatedPages(String initialUri) {
        return applyResilience(webClient.get().uri(initialUri).retrieve().bodyToMono(ConfluencePageDto.PaginatedResponse.class))
                .expand(response -> {
                    if (response.links() != null && response.links().next() != null) {
                        log.debug("Confluence API: загрузка следующей страницы...");
                        return applyResilience(webClient.get().uri(response.links().next()).retrieve().bodyToMono(ConfluencePageDto.PaginatedResponse.class));
                    } else {
                        return Mono.empty();
                    }
                })
                .flatMap(response -> Flux.fromIterable(response.results()));
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
