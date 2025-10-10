package com.example.ragollama.shared.exception;

import com.example.ragollama.shared.metrics.MetricService;
import com.example.ragollama.shared.task.TaskNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

/**
 * Глобальный обработчик исключений для Spring WebFlux, обеспечивающий
 * возврат ошибок в стандартизированном формате RFC 7807 (ProblemDetail).
 * <p>
 * Этот компонент перехватывает все исключения, возникшие в приложении,
 * преобразует их в семантически корректные HTTP-статусы и сериализует
 * тело ответа в валидный JSON, соблюдая контракт `application/problem+json`.
 */
@Component
@Order(-2) // Высокий приоритет, чтобы перехватывать ошибки раньше стандартных обработчиков Spring
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final MetricService metricService;
    private final ObjectMapper objectMapper;

    /**
     * Обрабатывает любое исключение, возникшее в цепочке обработки запроса.
     *
     * @param exchange Объект, инкапсулирующий запрос и ответ.
     * @param ex       Перехваченное исключение.
     * @return {@link Mono<Void>}, сигнализирующий о завершении записи ответа.
     */
    @Override
    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        ProblemDetail problemDetail = createProblemDetailForException(ex);

        // Логируем ошибку и инкрементируем метрики
        if (problemDetail.getStatus() >= 500) {
            log.error("Произошла внутренняя ошибка сервера ({}):", problemDetail.getTitle(), ex);
        } else if (!isClientAbortException(ex)) {
            log.warn("Ошибка обработки запроса ({}): {}", problemDetail.getTitle(), ex.getMessage());
        } else {
            // Не логируем разрыв соединения клиентом как ошибку
            log.info("Клиент разорвал соединение: {}", ex.getMessage());
            return Mono.empty();
        }

        metricService.incrementApiError(problemDetail.getStatus());

        // Устанавливаем статус и заголовки
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(problemDetail.getStatus()));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        // Сериализуем ProblemDetail в JSON и записываем в тело ответа
        return exchange.getResponse().writeWith(Mono.fromCallable(() -> {
                    try {
                        byte[] body = objectMapper.writeValueAsBytes(problemDetail);
                        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
                        return buffer;
                    } catch (JsonProcessingException e) {
                        log.error("Критическая ошибка: не удалось сериализовать ProblemDetail в JSON", e);
                        // Fallback на простой текст в случае ошибки сериализации
                        return exchange.getResponse().bufferFactory().wrap("Internal Server Error".getBytes(StandardCharsets.UTF_8));
                    }
                })
        );
    }

    /**
     * Создает объект {@link ProblemDetail} на основе типа исключения.
     *
     * @param ex Исключение.
     * @return Сконфигурированный объект ProblemDetail.
     */
    private ProblemDetail createProblemDetailForException(Throwable ex) {
        if (ex instanceof WebExchangeBindException e) {
            String errors = e.getBindingResult().getFieldErrors().stream()
                    .map(error -> String.format("'%s': %s", error.getField(), error.getDefaultMessage()))
                    .collect(Collectors.joining(", "));
            return createProblemDetail(HttpStatus.BAD_REQUEST, "Invalid Request Payload", "Ошибка валидации: " + errors);
        }
        if (ex instanceof ResponseStatusException e) {
            return createProblemDetail(e.getStatusCode(), e.getReason(), e.getMessage());
        }
        if (ex instanceof AccessDeniedException e) {
            return createProblemDetail(HttpStatus.FORBIDDEN, "Access Denied", e.getMessage());
        }
        if (ex instanceof PromptInjectionException e) {
            return createProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Prompt Injection Detected", e.getMessage());
        }
        if (ex instanceof QuotaExceededException e) {
            return createProblemDetail(HttpStatus.TOO_MANY_REQUESTS, "Quota Exceeded", e.getMessage());
        }
        if (ex instanceof RetrievalException e) {
            return createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, "Retrieval Service Error", "Сервис извлечения документов временно недоступен.");
        }
        if (ex instanceof GenerationException e) {
            return createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, "Generation Service Error", "Сервис генерации ответов временно недоступен.");
        }
        if (ex instanceof ProcessingException e) {
            return createProblemDetail(HttpStatus.BAD_GATEWAY, "AI Service Response Error", "Ошибка при обработке ответа от нижестоящего AI-сервиса.");
        }
        if (ex instanceof ResourceNotFoundException e) {
            return createProblemDetail(HttpStatus.NOT_FOUND, "Resource Not Found", e.getMessage());
        }
        if (ex instanceof TaskNotFoundException e) {
            return createProblemDetail(HttpStatus.NOT_FOUND, "Task Not Found", e.getMessage());
        }
        if (ex instanceof OptimisticLockingFailureException e) {
            return createProblemDetail(HttpStatus.CONFLICT, "Concurrent Modification", "Не удалось выполнить операцию из-за одновременного изменения данных. Пожалуйста, повторите попытку.");
        }
        if (ex instanceof GitOperationException e) {
            return createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, "Git Repository Unreachable", "Не удалось выполнить операцию с Git-репозиторием. Проверьте URL и учетные данные.");
        }
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Произошла внутренняя ошибка сервера.");
    }

    private ProblemDetail createProblemDetail(HttpStatusCode status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    private boolean isClientAbortException(Throwable ex) {
        if (ex instanceof CancellationException || ex instanceof IOException) {
            return true;
        }
        return ex.getCause() != null && (ex.getCause() instanceof CancellationException || ex.getCause() instanceof IOException);
    }
}
