package com.example.ragollama.rag.postprocessing;

import com.example.ragollama.rag.domain.model.RagAnswer;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.UUID;

/**
 * Контекстный объект, передающий все необходимые данные между этапами
 * постобработки RAG-ответа.
 *
 * @param requestId     Уникальный идентификатор исходного HTTP-запроса.
 * @param originalQuery Оригинальный, немодифицированный запрос пользователя.
 * @param documents     Список документов, фактически использованных для генерации контекста.
 * @param prompt        Финальный промпт, отправленный в LLM.
 * @param response      Финальный ответ, полученный от LLM, в виде "чистого" доменного объекта {@link RagAnswer}.
 * @param sessionId     Идентификатор сессии, к которой относится взаимодействие.
 */
public record RagProcessingContext(
        String requestId,
        String originalQuery,
        List<Document> documents,
        Prompt prompt,
        RagAnswer response,
        UUID sessionId
) {
}
