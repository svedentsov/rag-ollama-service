package com.example.ragollama.exception;

/**
 * Исключение, выбрасываемое при обнаружении в пользовательском вводе
 * потенциально вредоносной строки, нацеленной на атаку типа "Prompt Injection".
 * Это {@link RuntimeException}, так как оно сигнализирует о невалидных
 * данных со стороны клиента, которые должны быть обработаны на уровне API.
 */
public class PromptInjectionException extends RuntimeException {
    /**
     * Конструктор с сообщением об ошибке.
     *
     * @param message Детальное описание причины исключения.
     */
    public PromptInjectionException(String message) {
        super(message);
    }
}
