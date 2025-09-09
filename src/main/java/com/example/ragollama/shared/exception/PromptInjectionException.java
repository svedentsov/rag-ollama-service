package com.example.ragollama.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, выбрасываемое при обнаружении в пользовательском вводе
 * потенциально вредоносной строки, нацеленной на атаку типа "Prompt Injection".
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY) // 422 - семантически корректный, но неприемлемый запрос
public class PromptInjectionException extends RuntimeException {
    public PromptInjectionException(String message) {
        super(message);
    }
}
