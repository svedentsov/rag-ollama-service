package com.example.ragollama.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, выбрасываемое при возникновении ошибки на этапе генерации ответа (Generation).
 * Сигнализирует о проблемах при взаимодействии с LLM, таких как недоступность сервиса,
 * таймаут ответа или ошибки, возвращаемые самой моделью.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class GenerationException extends RuntimeException {
    /**
     * Конструктор с сообщением и причиной.
     *
     * @param message Детальное описание ошибки.
     * @param cause   Исходное исключение-причина.
     */
    public GenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
