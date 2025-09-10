package com.example.ragollama.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, выбрасываемое при обнаружении в пользовательском вводе
 * потенциально вредоносной строки, нацеленной на атаку типа "Prompt Injection".
 * <p>
 * Аннотация {@code @ResponseStatus} сообщает Spring MVC, что при возникновении
 * этого исключения в контроллере, необходимо вернуть клиенту HTTP-статус
 * 422 (Unprocessable Entity), который семантически корректен для данного случая.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class PromptInjectionException extends RuntimeException {
    /**
     * Конструктор, создающий исключение с заданным сообщением.
     *
     * @param message Сообщение, описывающее причину исключения.
     */
    public PromptInjectionException(String message) {
        super(message);
    }
}
