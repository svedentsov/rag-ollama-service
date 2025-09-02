package com.example.ragollama.shared.llm.model;

import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;

/**
 * Унифицированный DTO для ответа от шлюза {@link com.example.ragollama.shared.llm.LlmGateway}.
 * <p>
 * Этот record инкапсулирует не только текстовый ответ от языковой модели,
 * но и критически важные метаданные об использовании ресурсов (токены),
 * что является фундаментом для системы FinOps и контроля квот.
 *
 * @param content  Текстовое содержимое ответа, сгенерированное LLM.
 * @param usage    Объект с информацией о количестве использованных токенов
 *                 (на входе, на выходе, всего). Может быть {@code null}, если
 *                 модель не предоставляет эту информацию.
 * @param metadata Полные метаданные ответа от конкретной реализации Spring AI,
 *                 содержащие дополнительную информацию (например, причину остановки).
 */
public record LlmResponse(
        String content,
        Usage usage,
        ChatResponseMetadata metadata
) {
}
