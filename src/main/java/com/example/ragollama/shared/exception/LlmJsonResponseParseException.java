package com.example.ragollama.shared.exception;

import lombok.Getter;

/**
 * Исключение, выбрасываемое при ошибке парсинга JSON-ответа от LLM.
 *
 * <p>Это кастомное исключение позволяет централизованно обрабатывать
 * специфичный случай, когда языковая модель возвращает невалидный
 * или некорректно отформатированный JSON, который не удается
 * десериализовать даже после попыток очистки.
 */
@Getter
public class LlmJsonResponseParseException extends ProcessingException {

    /**
     * "Сырой" JSON-ответ от LLM, который не удалось распарсить.
     * Может быть полезен для логирования и последующей отладки.
     */
    private final String rawResponse;

    /**
     * Конструктор с сообщением, причиной и "сырым" ответом.
     *
     * @param message     Детальное описание ошибки.
     * @param cause       Исходное исключение-причина (обычно {@code JsonProcessingException}).
     * @param rawResponse "Сырой" ответ от LLM для логирования.
     */
    public LlmJsonResponseParseException(String message, Throwable cause, String rawResponse) {
        super(message, cause);
        this.rawResponse = rawResponse;
    }
}
