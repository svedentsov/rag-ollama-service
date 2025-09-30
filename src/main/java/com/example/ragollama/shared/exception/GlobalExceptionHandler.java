package com.example.ragollama.shared.exception;

import com.example.ragollama.shared.metrics.MetricService;
import com.example.ragollama.shared.task.TaskNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Глобальный обработчик исключений, реализующий централизованную и
 * унифицированную логику обработки ошибок для всего приложения.
 * <p>
 * Этот компонент перехватывает все исключения, возникающие в контроллерах,
 * и преобразует их в стандартизированные ответы в формате {@link ProblemDetail}
 * (RFC 7807), что соответствует лучшим практикам построения REST API.
 * <p>
 * Каждый обработчик также инкрементирует соответствующую метрику Micrometer,
 * обеспечивая наблюдаемость за состоянием ошибок в системе.
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MetricService metricService;

    /**
     * Обрабатывает исключения, когда запрашиваемый статический ресурс или API эндпоинт не найден.
     * Логирует это событие с уровнем WARN, чтобы не засорять логи ошибками ERROR.
     *
     * @param e Исключение NoResourceFoundException.
     * @return ProblemDetail с кодом 404 Not Found.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFoundException(NoResourceFoundException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setProperty("timestamp", Instant.now());
        metricService.incrementApiError(HttpStatus.NOT_FOUND.value());
        log.warn("Запрошен несуществующий ресурс: {}", e.getResourcePath());
        return problemDetail;
    }

    /**
     * Обрабатывает исключения валидации DTO (аннотации @Valid).
     * Собирает все ошибки валидации в одно читаемое сообщение.
     *
     * @param e Исключение MethodArgumentNotValidException.
     * @return ProblemDetail с кодом 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("'%s': %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Ошибка валидации: " + errors);
        problemDetail.setTitle("Invalid Request Payload");
        problemDetail.setProperty("timestamp", Instant.now());
        metricService.incrementApiError(HttpStatus.BAD_REQUEST.value());
        log.warn("Ошибка валидации: {}", errors);
        return problemDetail;
    }

    /**
     * Обрабатывает наше кастомное исключение AccessDeniedException.
     *
     * @param e Исключение.
     * @return ProblemDetail с кодом 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
        problemDetail.setTitle("Access Denied");
        problemDetail.setProperty("timestamp", Instant.now());
        metricService.incrementApiError(HttpStatus.FORBIDDEN.value());
        log.warn("Отказано в доступе: {}", e.getMessage());
        return problemDetail;
    }

    /**
     * Обрабатывает кастомное исключение PromptInjectionException.
     *
     * @param e Исключение.
     * @return ProblemDetail с кодом 422 (Unprocessable Entity).
     */
    @ExceptionHandler(PromptInjectionException.class)
    public ProblemDetail handlePromptInjectionException(PromptInjectionException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problemDetail.setTitle("Prompt Injection Detected");
        problemDetail.setProperty("timestamp", Instant.now());
        metricService.incrementApiError(HttpStatus.UNPROCESSABLE_ENTITY.value());
        log.warn("Попытка Prompt Injection заблокирована. Запрос: [{}]. Причина: {}", e.getOffendingQuery(), e.getMessage());
        return problemDetail;
    }

    /**
     * Обрабатывает исключение при превышении квоты.
     *
     * @param e Исключение.
     * @return ProblemDetail с кодом 429.
     */
    @ExceptionHandler(QuotaExceededException.class)
    public ProblemDetail handleQuotaExceededException(QuotaExceededException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        problemDetail.setTitle("Quota Exceeded");
        problemDetail.setProperty("timestamp", Instant.now());
        metricService.incrementApiError(HttpStatus.TOO_MANY_REQUESTS.value());
        log.warn("Пользователь превысил квоту: {}", e.getMessage());
        return problemDetail;
    }

    /**
     * Обрабатывает ошибки на этапе извлечения.
     *
     * @param e Исключение.
     * @return ProblemDetail с кодом 503.
     */
    @ExceptionHandler(RetrievalException.class)
    public ProblemDetail handleRetrievalException(RetrievalException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Сервис извлечения документов временно недоступен. Попробуйте позже.");
        problemDetail.setTitle("Retrieval Service Error");
        problemDetail.setProperty("timestamp", Instant.now());
        metricService.incrementApiError(HttpStatus.SERVICE_UNAVAILABLE.value());
        log.error("Ошибка Retrieval-сервиса: {}", e.getMessage(), e.getCause());
        return problemDetail;
    }

    /**
     * Обрабатывает ошибки на этапе генерации.
     *
     * @param e Исключение.
     * @return ProblemDetail с кодом 503.
     */
    @ExceptionHandler(GenerationException.class)
    public ProblemDetail handleGenerationException(GenerationException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Сервис генерации ответов временно недоступен. Попробуйте позже.");
        problemDetail.setTitle("Generation Service Error");
        problemDetail.setProperty("timestamp", Instant.now());
        metricService.incrementApiError(HttpStatus.SERVICE_UNAVAILABLE.value());
        log.error("Ошибка Generation-сервиса: {}", e.getMessage(), e.getCause());
        return problemDetail;
    }

    /**
     * Обрабатывает ошибки при обработке ответа от LLM.
     *
     * @param e Исключение.
     * @return ProblemDetail с кодом 502.
     */
    @ExceptionHandler(ProcessingException.class)
    public ProblemDetail handleProcessingException(ProcessingException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Ошибка при обработке ответа от нижестоящего AI-сервиса.");
        problemDetail.setTitle("AI Service Response Error");
        problemDetail.setProperty("timestamp", Instant.now());
        metricService.incrementApiError(HttpStatus.BAD_GATEWAY.value());
        log.error("Ошибка обработки: {}", e.getMessage(), e.getCause());
        return problemDetail;
    }

    /**
     * Обрабатывает стандартное исключение EntityNotFoundException.
     *
     * @param e Исключение.
     * @return ProblemDetail с кодом 404.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFoundException(EntityNotFoundException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problemDetail.setTitle("Entity Not Found");
        problemDetail.setProperty("timestamp", Instant.now());
        metricService.incrementApiError(HttpStatus.NOT_FOUND.value());
        log.warn("Запрошена несуществующая сущность: {}", e.getMessage());
        return problemDetail;
    }

    /**
     * Обрабатывает ошибку, когда асинхронная задача не найдена.
     *
     * @param e Исключение TaskNotFoundException.
     * @return ProblemDetail с кодом 404.
     */
    @ExceptionHandler(TaskNotFoundException.class)
    public ProblemDetail handleTaskNotFoundException(TaskNotFoundException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problemDetail.setTitle("Task Not Found");
        problemDetail.setProperty("timestamp", Instant.now());
        metricService.incrementApiError(HttpStatus.NOT_FOUND.value());
        log.warn("Запрошена несуществующая задача: {}", e.getMessage());
        return problemDetail;
    }

    /**
     * Обрабатывает все остальные непредвиденные исключения.
     *
     * @param e Исключение.
     * @return ProblemDetail с кодом 500.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Произошла внутренняя ошибка сервера.");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("timestamp", Instant.now());
        metricService.incrementApiError(HttpStatus.INTERNAL_SERVER_ERROR.value());
        log.error("Произошла непредвиденная ошибка: {}", e.getMessage(), e);
        return problemDetail;
    }
}
