package com.example.ragollama.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Кастомное исключение, выбрасываемое, когда запрашиваемый ресурс (сущность) не найден.
 * Заменяет собой EntityNotFoundException из пакета jakarta.persistence.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Конструктор с сообщением об ошибке.
     *
     * @param message Детальное описание, какой ресурс не был найден.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
