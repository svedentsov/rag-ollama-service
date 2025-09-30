package com.example.ragollama.shared.task;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, выбрасываемое при попытке доступа к задаче,
 * которая не существует в реестре.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class TaskNotFoundException extends RuntimeException {
    /**
     * Конструктор с сообщением об ошибке.
     *
     * @param message Детальное описание причины.
     */
    public TaskNotFoundException(String message) {
        super(message);
    }
}
