package com.example.ragollama.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, выбрасываемое, когда пользователь исчерпал свою квоту на использование LLM.
 * Аннотация {@code @ResponseStatus} обеспечивает возврат корректного HTTP-кода 429 (Too Many Requests).
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class QuotaExceededException extends RuntimeException {
    /**
     * Конструктор с сообщением об ошибке.
     * @param message Детальное описание причины.
     */
    public QuotaExceededException(String message) {
        super(message);
    }
}
