package com.example.ragollama.rag.pipeline;

import com.example.ragollama.rag.agent.ProcessedQueries;
import com.example.ragollama.rag.domain.model.RagAnswer;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Унифицированный, неизменяемый объект-контейнер состояния RAG-конвейера.
 * Хранит все промежуточные результаты каждого шага, обеспечивая прозрачность и
 * возможность для последующих шагов использовать результаты предыдущих.
 *
 * @param originalQuery       Исходный, немодифицированный запрос пользователя.
 * @param history             История сообщений из текущей сессии чата.
 * @param topK                Параметр `topK` для векторного поиска.
 * @param similarityThreshold Параметр `similarityThreshold` для векторного поиска.
 * @param sessionId           Идентификатор текущей сессии чата.
 * @param promptModel         Карта для динамического построения финального промпта.
 * @param userProvidedContext Текстовый контекст, явно предоставленный пользователем (например, из файлов).
 * @param processedQueries    Результат работы конвейера обработки запросов.
 * @param retrievedDocuments  Список документов после этапа извлечения.
 * @param rerankedDocuments   Список документов после этапа переранжирования.
 * @param compressedContext   Сжатый текстовый контекст после этапа компрессии.
 * @param finalPrompt         Финальный объект промпта, готовый для отправки в LLM.
 * @param finalAnswer         Финальный объект ответа от RAG-системы.
 */
public record RagFlowContext(
        String originalQuery,
        List<Message> history,
        int topK,
        double similarityThreshold,
        UUID sessionId,
        Map<String, Object> promptModel,
        String userProvidedContext,
        ProcessedQueries processedQueries,
        List<Document> retrievedDocuments,
        List<Document> rerankedDocuments,
        String compressedContext,
        Prompt finalPrompt,
        RagAnswer finalAnswer
) {
    /**
     * Конструктор для инициализации конвейера с базовыми параметрами.
     */
    public RagFlowContext(String originalQuery, List<Message> history, int topK, double similarityThreshold, UUID sessionId) {
        this(originalQuery, history, topK, similarityThreshold, sessionId, new ConcurrentHashMap<>(), null, null, List.of(), List.of(), null, null, null);
    }

    public RagFlowContext withUserProvidedContext(String context) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, context, this.processedQueries, this.retrievedDocuments, this.rerankedDocuments, this.compressedContext, this.finalPrompt, this.finalAnswer);
    }

    public RagFlowContext withProcessedQueries(ProcessedQueries queries) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, this.userProvidedContext, queries, this.retrievedDocuments, this.rerankedDocuments, this.compressedContext, this.finalPrompt, this.finalAnswer);
    }

    public RagFlowContext withRetrievedDocuments(List<Document> documents) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, this.userProvidedContext, this.processedQueries, documents, this.rerankedDocuments, this.compressedContext, this.finalPrompt, this.finalAnswer);
    }

    public RagFlowContext withRerankedDocuments(List<Document> documents) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, this.userProvidedContext, this.processedQueries, this.retrievedDocuments, documents, this.compressedContext, this.finalPrompt, this.finalAnswer);
    }

    public RagFlowContext withCompressedContext(String compressedContext) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, this.userProvidedContext, this.processedQueries, this.retrievedDocuments, this.rerankedDocuments, compressedContext, this.finalPrompt, this.finalAnswer);
    }

    public RagFlowContext withFinalPrompt(Prompt prompt) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, this.userProvidedContext, this.processedQueries, this.retrievedDocuments, this.rerankedDocuments, this.compressedContext, prompt, this.finalAnswer);
    }

    public RagFlowContext withFinalAnswer(RagAnswer answer) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, this.userProvidedContext, this.processedQueries, this.retrievedDocuments, this.rerankedDocuments, this.compressedContext, this.finalPrompt, answer);
    }
}
