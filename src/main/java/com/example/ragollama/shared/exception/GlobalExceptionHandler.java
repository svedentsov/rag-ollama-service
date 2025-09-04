package com.example.ragollama.shared.exception;

import com.example.ragollama.shared.metrics.MetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Глобальный обработчик исключений, реализующий централизованную и
 * унифицированную логику обработки ошибок для всего приложения.
 *
 * <p>Этот компонент перехватывает исключения, выброшенные из любого контроллера,
 * логирует их, записывает метрики и возвращает клиенту стандартизированный
 * ответ об ошибке в формате {@link ProblemDetail} (RFC 7807), скрывая
 * детали внутренней реализации и stack trace.
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MetricService metricService;

    /**
     * Обрабатывает исключения {@link MethodArgumentNotValidException}, которые возникают
     * при сбое валидации DTO (аннотации {@code @Valid}, {@code @NotBlank}, и т.д.).
     *
     * @param e Исключение, содержащее информацию об ошибках валидации.
     * @return {@link ProblemDetail} с кодом 400 (Bad Request) и детальным описанием ошибок.
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
     * Обрабатывает кастомное исключение {@link PromptInjectionException}, выбрасываемое
     * при обнаружении потенциальной атаки "Prompt Injection".
     *
     * @param e Исключение {@link PromptInjectionException}.
     * @return {@link ProblemDetail} с кодом 422 (Unprocessable Entity), информирующий клиента о причине блокировки.
     */
    @ExceptionHandler(PromptInjectionException.class)
    public ProblemDetail handlePromptInjectionException(PromptInjectionException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problemDetail.setTitle("Prompt Injection Detected");
        problemDetail.setProperty("timestamp", Instant.now());
        metricService.incrementApiError(HttpStatus.UNPROCESSABLE_ENTITY.value());
        log.warn("Попытка Prompt Injection заблокирована: {}", e.getMessage());
        return problemDetail;
    }

    /**
     * Обрабатывает исключение {@link QuotaExceededException}, возникающее при
     * превышении пользователем лимита на использование LLM.
     *
     * @param e Исключение {@link QuotaExceededException}.
     * @return {@link ProblemDetail} с кодом 429 (Too Many Requests).
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
     * Обрабатывает исключения, возникшие на этапе извлечения документов (Retrieval).
     *
     * @param e Исключение {@link RetrievalException}.
     * @return {@link ProblemDetail} с кодом 503 (Service Unavailable).
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
     * Обрабатывает исключения, возникшие на этапе генерации ответа (Generation).
     *
     * @param e Исключение {@link GenerationException}.
     * @return {@link ProblemDetail} с кодом 503 (Service Unavailable).
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
     * Обрабатывает исключения, связанные с ошибками в фоновой обработке,
     * например, при парсинге ответа от LLM.
     *
     * @param e Исключение {@link ProcessingException}.
     * @return {@link ProblemDetail} с кодом 502 (Bad Gateway), так как
     * ошибка произошла при взаимодействии с внешней системой (LLM).
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
     * Обрабатывает все остальные, непредвиденные исключения.
     *
     * @param e Любое необработанное исключение.
     * @return {@link ProblemDetail} с кодом 500 (Internal Server Error).
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
