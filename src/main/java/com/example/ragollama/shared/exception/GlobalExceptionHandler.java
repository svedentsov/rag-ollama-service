package com.example.ragollama.shared.exception;

import com.example.ragollama.shared.metrics.MetricService;
import com.example.ragollama.shared.task.TaskNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

/**
 * Глобальный обработчик исключений для Spring WebFlux.
 * <p>
 * Эта реализация заменяет {@code @RestControllerAdvice} и является нативным
 * способом обработки ошибок в реактивном стеке. Она перехватывает все исключения,
 * возникающие в цепочке обработки запроса, и формирует стандартизированный
 * ответ в формате {@link ProblemDetail} (RFC 7807).
 */
@Component
@Order(-2) // Высокий приоритет, чтобы перехватывать ошибки раньше стандартных обработчиков
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final MetricService metricService;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ProblemDetail problemDetail;

        if (ex instanceof WebExchangeBindException e) {
            problemDetail = handleWebExchangeBindException(e);
        } else if (ex instanceof ResponseStatusException e) {
            // Включает 404 Not Found и другие HTTP-статусные исключения WebFlux
            problemDetail = handleResponseStatusException(e);
        } else if (isClientAbortException(ex)) {
            log.info("Клиент разорвал соединение: {}", ex.getMessage());
            return Mono.empty(); // Просто завершаем обработку
        } else if (ex instanceof AccessDeniedException e) {
            problemDetail = createProblemDetail(HttpStatus.FORBIDDEN, "Access Denied", e.getMessage());
        } else if (ex instanceof PromptInjectionException e) {
            problemDetail = createProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Prompt Injection Detected", e.getMessage());
        } else if (ex instanceof QuotaExceededException e) {
            problemDetail = createProblemDetail(HttpStatus.TOO_MANY_REQUESTS, "Quota Exceeded", e.getMessage());
        } else if (ex instanceof RetrievalException e) {
            problemDetail = createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, "Retrieval Service Error", "Сервис извлечения документов временно недоступен.");
        } else if (ex instanceof GenerationException e) {
            problemDetail = createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, "Generation Service Error", "Сервис генерации ответов временно недоступен.");
        } else if (ex instanceof ProcessingException e) {
            problemDetail = createProblemDetail(HttpStatus.BAD_GATEWAY, "AI Service Response Error", "Ошибка при обработке ответа от нижестоящего AI-сервиса.");
        } else if (ex instanceof EntityNotFoundException e) {
            problemDetail = createProblemDetail(HttpStatus.NOT_FOUND, "Entity Not Found", e.getMessage());
        } else if (ex instanceof TaskNotFoundException e) {
            problemDetail = createProblemDetail(HttpStatus.NOT_FOUND, "Task Not Found", e.getMessage());
        } else {
            problemDetail = createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Произошла внутренняя ошибка сервера.");
            log.error("Произошла непредвиденная ошибка: {}", ex.getMessage(), ex);
        }

        metricService.incrementApiError(problemDetail.getStatus());
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(problemDetail.getStatus()));
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(problemDetail.toString().getBytes()))
        );
    }

    private ProblemDetail handleWebExchangeBindException(WebExchangeBindException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("'%s': %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        log.warn("Ошибка валидации: {}", errors);
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Invalid Request Payload", "Ошибка валидации: " + errors);
    }

    private ProblemDetail handleResponseStatusException(ResponseStatusException e) {
        log.warn("Запрошен несуществующий ресурс или возникла другая HTTP-ошибка: {}", e.getMessage());
        return createProblemDetail(e.getStatusCode(), e.getReason(), e.getMessage());
    }

    private ProblemDetail createProblemDetail(HttpStatusCode status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    /**
     * Проверяет, является ли исключение следствием разрыва соединения клиентом.
     *
     * @param ex Исключение.
     * @return {@code true}, если это ошибка разрыва соединения.
     */
    private boolean isClientAbortException(Throwable ex) {
        if (ex instanceof CancellationException || ex instanceof IOException) {
            return true;
        }
        return ex.getCause() != null && (ex.getCause() instanceof CancellationException || ex.getCause() instanceof IOException);
    }
}
