package com.example.ragollama.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, выбрасываемое при обнаружении в пользовательском вводе
 * потенциально вредоносной строки, нацеленной на атаку типа "Prompt Injection".
 * <p> Аннотация {@code @ResponseStatus} сообщает Spring MVC, что при возникновении
 * этого исключения в контроллере, необходимо вернуть клиенту HTTP-статус
 * 422 (Unprocessable Entity), который семантически корректен для данного случая.
 */
@Getter
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class PromptInjectionException extends RuntimeException {

    /**
     * Запрос, который был признан потенциально вредоносным.
     */
    private final String offendingQuery;

    /**
     * Конструктор, создающий исключение с заданным сообщением и сохраняющий
     * исходный запрос.
     *
     * @param message        Сообщение, описывающее причину исключения.
     * @param offendingQuery Запрос, вызвавший срабатывание защиты.
     */
    public PromptInjectionException(String message, String offendingQuery) {
        super(message);
        this.offendingQuery = offendingQuery;
    }
}
