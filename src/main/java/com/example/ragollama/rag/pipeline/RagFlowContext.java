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
 * Добавлено поле для хранения сжатого контекста.
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
        String compressedContext,
        Prompt finalPrompt,
        RagAnswer finalAnswer
) {
    /**
     * Основной конструктор для инициализации конвейера.
     */
    public RagFlowContext(String originalQuery, List<Message> history, int topK, double similarityThreshold, UUID sessionId) {
        this(originalQuery, history, topK, similarityThreshold, sessionId, new ConcurrentHashMap<>(), null, List.of(), List.of(), null, null, null);
    }

    public RagFlowContext withProcessedQueries(ProcessedQueries queries) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, queries, this.retrievedDocuments, this.rerankedDocuments, this.compressedContext, this.finalPrompt, this.finalAnswer);
    }

    public RagFlowContext withRetrievedDocuments(List<Document> documents) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, this.processedQueries, documents, this.rerankedDocuments, this.compressedContext, this.finalPrompt, this.finalAnswer);
    }

    public RagFlowContext withRerankedDocuments(List<Document> documents) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, this.processedQueries, this.retrievedDocuments, documents, this.compressedContext, this.finalPrompt, this.finalAnswer);
    }

    public RagFlowContext withCompressedContext(String compressedContext) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, this.processedQueries, this.retrievedDocuments, this.rerankedDocuments, compressedContext, this.finalPrompt, this.finalAnswer);
    }

    public RagFlowContext withFinalPrompt(Prompt prompt) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, this.processedQueries, this.retrievedDocuments, this.rerankedDocuments, this.compressedContext, prompt, this.finalAnswer);
    }

    public RagFlowContext withFinalAnswer(RagAnswer answer) {
        return new RagFlowContext(this.originalQuery, this.history, this.topK, this.similarityThreshold, this.sessionId, this.promptModel, this.processedQueries, this.retrievedDocuments, this.rerankedDocuments, this.compressedContext, this.finalPrompt, answer);
    }
}
