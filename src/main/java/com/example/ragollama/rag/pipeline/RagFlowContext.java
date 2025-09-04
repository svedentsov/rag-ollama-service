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
 * Унифицированный, неизменяемый (immutable) объект-контейнер, представляющий
 * полное состояние RAG-конвейера на любом этапе его выполнения.
 * <p>
 * Этот объект является ЕДИНСТВЕННЫМ DTO состояния, который передается между
 * всеми компонентами RAG-конвейера, включая "Советников" (Advisors) и "Шаги" (Steps).
 * Он накапливает результаты по мере продвижения по конвейеру.
 *
 * @param originalQuery       Исходный запрос пользователя.
 * @param history             История чата.
 * @param topK                Параметр topK.
 * @param similarityThreshold Порог схожести.
 * @param sessionId           ID сессии.
 * @param promptModel         Карта для построения финального промпта.
 * @param processedQueries    Результат работы {@code QueryProcessingPipeline}.
 * @param retrievedDocuments  Документы после этапа Retrieval.
 * @param rerankedDocuments   Документы после этапа Reranking.
 * @param finalPrompt         Финальный промпт для LLM.
 * @param finalAnswer         Финальный ответ от RAG-системы.
 */
public record RagFlowContext(
        String originalQuery,
        List<Message> history,
        int topK,
        double similarityThreshold,
        UUID sessionId,
        Map<String, Object> promptModel,
        ProcessedQueries processedQueries,
        List<Document> retrievedDocuments,
        List<Document> rerankedDocuments,
        Prompt finalPrompt,
        RagAnswer finalAnswer
) {
    /**
     * Основной конструктор для инициализации конвейера.
     */
    public RagFlowContext(String originalQuery, List<Message> history, int topK, double similarityThreshold, UUID sessionId) {
        this(originalQuery, history, topK, similarityThreshold, sessionId, new ConcurrentHashMap<>(), null, List.of(), List.of(), null, null);
    }

    public RagFlowContext withProcessedQueries(ProcessedQueries queries) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, queries, this.retrievedDocuments, this.rerankedDocuments, this.finalPrompt, this.finalAnswer);
    }

    public RagFlowContext withRetrievedDocuments(List<Document> documents) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, this.processedQueries, documents, this.rerankedDocuments, this.finalPrompt, this.finalAnswer);
    }

    public RagFlowContext withRerankedDocuments(List<Document> documents) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, this.processedQueries, this.retrievedDocuments, documents, this.finalPrompt, this.finalAnswer);
    }

    public RagFlowContext withFinalPrompt(Prompt prompt) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, this.processedQueries, this.retrievedDocuments, this.rerankedDocuments, prompt, this.finalAnswer);
    }

    public RagFlowContext withFinalAnswer(RagAnswer answer) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, this.processedQueries, this.retrievedDocuments, this.rerankedDocuments, this.finalPrompt, answer);
    }
}
