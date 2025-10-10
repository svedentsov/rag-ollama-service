package com.example.ragollama.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, выбрасываемое при возникновении ошибки во время выполнения
 * операции с Git-репозиторием (например, clone, diff, blame).
 * <p>
 * Это кастомное исключение позволяет централизованно обрабатывать все сбои,
 * связанные с JGit, и предоставлять клиенту API осмысленную обратную связь.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class GitOperationException extends RuntimeException {

    /**
     * Конструктор с сообщением и причиной.
     *
     * @param message Детальное описание ошибки.
     * @param cause   Исходное исключение-причина (обычно от библиотеки JGit).
     */
    public GitOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
