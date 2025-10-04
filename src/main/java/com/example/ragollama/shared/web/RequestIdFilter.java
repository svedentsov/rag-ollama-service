package com.example.ragollama.shared.web;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * Реактивный веб-фильтр для добавления уникального ID к каждому HTTP-запросу.
 * <p>
 * Эта реализация адаптирована для Spring WebFlux и заменяет собой
 * сервлет-фильтр. Она использует {@link WebFilter} для интеграции в
 * неблокирующую цепочку обработки запросов.
 * <p>
 * Ключевое отличие — для распространения ID запроса в асинхронной среде
 * используется <b>контекст Reactor</b> (`contextWrite`), который автоматически
 * пробрасывается в MDC благодаря `Hooks.enableAutomaticContextPropagation()`.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter implements WebFilter {

    /**
     * Имя HTTP-заголовка, в котором передается ID запроса.
     */
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    /**
     * Ключ, используемый для сохранения ID запроса в MDC и контексте Reactor.
     */
    private static final String MDC_KEY = "requestId";

    /**
     * Основной метод фильтра, выполняемый для каждого запроса в реактивной цепочке.
     *
     * @param exchange Объект, инкапсулирующий HTTP-запрос и ответ.
     * @param chain    Цепочка фильтров.
     * @return {@link Mono}, сигнализирующий о завершении обработки.
     */
    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        // Передаем requestId в цепочку через контекст Reactor.
        // Spring Boot автоматически пробросит его в MDC для логирования.
        final String finalRequestId = requestId;
        return chain.filter(exchange)
                .contextWrite(Context.of(MDC_KEY, finalRequestId))
                // Очистка MDC после завершения (на всякий случай, хотя Context Propagation должен справляться)
                .doFinally(signalType -> MDC.remove(MDC_KEY));
    }
}
