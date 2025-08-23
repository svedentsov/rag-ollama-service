package com.example.ragollama.shared.exception;

/**
 * Общее кастомное исключение для ошибок, возникающих в процессе
 * асинхронной обработки (например, в воркерах).
 * <p>
 * Это непроверяемое (unchecked) исключение используется для сигнализации
 * о критических, но ожидаемых сбоях в фоновых задачах.
 */
public class ProcessingException extends RuntimeException {
    /**
     * Конструктор с сообщением об ошибке.
     *
     * @param message Детальное описание причины сбоя.
     */
    public ProcessingException(String message) {
        super(message);
    }

    /**
     * Конструктор с сообщением и причиной.
     *
     * @param message Детальное описание ошибки.
     * @param cause   Исходное исключение-причина.
     */
    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
