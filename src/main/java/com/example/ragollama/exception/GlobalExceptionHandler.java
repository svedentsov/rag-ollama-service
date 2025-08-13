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
 * Перехватывает исключения и формирует стандартизированные ответы об ошибках
 * в формате Problem Details for HTTP APIs (RFC 7807).
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Обрабатывает исключения, связанные с ошибками валидации DTO.
     * @param e Исключение {@link MethodArgumentNotValidException}.
     * @return {@link ProblemDetail} с кодом 400 (Bad Request).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("'%s': %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Ошибка валидации: " + errors);
        problemDetail.setTitle("Invalid Request Payload");
        problemDetail.setProperty("timestamp", Instant.now());
        log.warn("Validation error: {}", errors, e);
        return problemDetail;
    }

    /**
     * Обрабатывает кастомное исключение {@link PromptInjectionException}.
     * @param e Исключение {@link PromptInjectionException}.
     * @return {@link ProblemDetail} с кодом 422 (Unprocessable Entity).
     */
    @ExceptionHandler(PromptInjectionException.class)
    public ProblemDetail handlePromptInjectionException(PromptInjectionException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problemDetail.setTitle("Prompt Injection Detected");
        problemDetail.setProperty("timestamp", Instant.now());
        log.warn("Prompt injection attempt blocked: {}", e.getMessage());
        return problemDetail;
    }

    /**
     * Обрабатывает все остальные непредвиденные исключения.
     * @param e Любое исключение типа {@link Exception}.
     * @return {@link ProblemDetail} с кодом 500 (Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Произошла внутренняя ошибка сервера.");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("timestamp", Instant.now());
        log.error("An unexpected error occurred: {}", e.getMessage(), e);
        return problemDetail;
    }
}
