package com.example.ragollama.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Глобальный обработчик исключений для всего приложения.
 * <p>
 * Перехватывает исключения, возникающие в контроллерах, и формирует
 * стандартизированные ответы об ошибках в формате Problem Details for HTTP APIs (RFC 7807).
 * Это обеспечивает консистентный формат ошибок для всех API-клиентов.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

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
        log.warn("Попытка Prompt Injection заблокирована: {}", e.getMessage());
        return problemDetail;
    }

    /**
     * Обрабатывает все остальные непредвиденные исключения, которые не были перехвачены
     * другими обработчиками. Это "catch-all" механизм.
     *
     * @param e Любое необработанное исключение типа {@link Exception}.
     * @return {@link ProblemDetail} с кодом 500 (Internal Server Error) и общим сообщением об ошибке.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Произошла внутренняя ошибка сервера.");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("timestamp", Instant.now());
        log.error("Произошла непредвиденная ошибка: {}", e.getMessage(), e);
        return problemDetail;
    }
}
