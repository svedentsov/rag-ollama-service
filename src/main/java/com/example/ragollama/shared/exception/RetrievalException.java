package com.example.ragollama.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, выбрасываемое при возникновении ошибки на этапе извлечения данных (Retrieval).
 * Сигнализирует о проблемах при взаимодействии с векторным хранилищем,
 * таких как сетевые ошибки, таймауты или некорректные ответы от базы данных.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class RetrievalException extends RuntimeException {
    /**
     * Конструктор с сообщением и причиной.
     *
     * @param message Детальное описание ошибки.
     * @param cause   Исходное исключение-причина.
     */
    public RetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}
