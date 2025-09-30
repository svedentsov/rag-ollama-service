package com.example.ragollama.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, выбрасываемое, когда аутентифицированный пользователь
 * пытается выполнить действие, на которое у него нет прав.
 * <p>
 * Аннотация {@code @ResponseStatus(HttpStatus.FORBIDDEN)} сообщает Spring MVC,
 * что при возникновении этого исключения необходимо вернуть клиенту
 * HTTP-статус 403 (Forbidden), что является семантически корректным
 * для данного типа ошибок.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException {

    /**
     * Конструктор с сообщением об ошибке.
     *
     * @param message Детальное описание причины отказа в доступе.
     */
    public AccessDeniedException(String message) {
        super(message);
    }
}
