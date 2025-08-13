package com.example.ragollama.exception;

/**
 * Исключение, выбрасываемое при обнаружении потенциальной атаки Prompt Injection.
 */
public class PromptInjectionException extends RuntimeException {
    public PromptInjectionException(String message) {
        super(message);
    }
}
