package com.example.ragollama.service;

import com.example.ragollama.dto.RagQueryRequest;
import com.example.ragollama.dto.RagQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Сервис для выполнения RAG (Retrieval-Augmented Generation) запросов.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final PromptGuardService promptGuardService;

    @Value("""
            Вы — ассистент, отвечающий на вопросы.
            Используйте только предоставленные ниже фрагменты информации (контекст) для ответа на вопрос пользователя.
            Если в контексте нет ответа, просто скажите, что вы не знаете. Не пытайтесь придумать ответ.
            Отвечайте на русском языке.

            Контекст:
            {context}

            Вопрос:
            {question}
            """)
    private String ragPromptTemplate;

    public RagQueryResponse query(RagQueryRequest request) {
        log.info("Received RAG query: '{}'", request.query());
        promptGuardService.checkForInjection(request.query());

        // 1. Retrieval: строим SearchRequest через builder()
        SearchRequest searchRequest = SearchRequest.builder()
                .query(request.query())
                .topK(request.topK())
                .similarityThreshold(request.similarityThreshold())
                .build();

        List<Document> similarDocuments = vectorStore.similaritySearch(searchRequest);
        log.info("Found {} similar documents for the query.", similarDocuments.size());

        String context = similarDocuments.stream()
                .map(Document::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n---\n"));

        // 2. Generation
        PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);
        Prompt prompt = promptTemplate.create(Map.of(
                "context", context,
                "question", request.query()
        ));

        log.debug("Generated RAG prompt for LLM.");
        // Используем fluent ChatClient API: prompt(...).call().content()
        String generatedAnswer = chatClient.prompt(prompt)
                .call()
                .content();

        List<String> sourceCitations = similarDocuments.stream()
                .map(doc -> (String) doc.getMetadata().getOrDefault("source", "Unknown"))
                .distinct()
                .toList();
        log.info("Generated answer. Used sources: {}", sourceCitations);

        return new RagQueryResponse(generatedAnswer, sourceCitations);
    }
}
