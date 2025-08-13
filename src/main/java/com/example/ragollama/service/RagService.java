package com.example.ragollama.service;

import com.example.ragollama.dto.RagQueryRequest;
import com.example.ragollama.dto.RagQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final ResilientAiClient resilientAiClient;
    private final PromptGuardService promptGuardService;
    private final PromptService promptService;
    private final MetricService metricService;
    private final Optional<RerankingService> rerankingService;

    @Cacheable(value = "vector_search_results", key = "#request.query() + '_' + #request.topK() + '_' + #request.similarityThreshold()")
    public RagQueryResponse query(RagQueryRequest request) {
        log.info("Received RAG query: '{}'", request.query());
        metricService.incrementCacheMiss(); // Инкрементируем промах, т.к. если мы тут, кэш не сработал
        promptGuardService.checkForInjection(request.query());

        // 1. Retrieval
        SearchRequest searchRequest = SearchRequest.builder()
                .query(request.query())
                .topK(request.topK())
                .similarityThreshold(request.similarityThreshold())
                .build();

        List<Document> similarDocuments = executeSimilaritySearch(searchRequest);

        // 2. Reranking (optional)
        if (rerankingService.isPresent()) {
            similarDocuments = rerankingService.get().rerank(similarDocuments, request.query());
            log.info("Reranked documents. Top document similarity: {}",
                    similarDocuments.isEmpty() ? "N/A" : similarDocuments.get(0).getMetadata().get("rerankedSimilarity"));
        }

        String context = similarDocuments.stream()
                .map(Document::getText)
                .collect(java.util.stream.Collectors.joining("\n---\n"));

        // 3. Generation
        String promptString = promptService.createRagPrompt(Map.of(
                "context", context,
                "question", request.query()
        ));

        String generatedAnswer = executeChatCall(new Prompt(promptString));

        List<String> sourceCitations = similarDocuments.stream()
                .map(doc -> (String) doc.getMetadata().getOrDefault("source", "Unknown"))
                .distinct()
                .toList();
        log.info("Generated answer. Used sources: {}", sourceCitations);

        return new RagQueryResponse(generatedAnswer, sourceCitations);
    }

    private List<Document> executeSimilaritySearch(SearchRequest searchRequest) {
        try {
            //noinspection unchecked
            return (List<Document>) resilientAiClient.similaritySearch(searchRequest).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error during similarity search", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to retrieve documents from vector store", e);
        }
    }

    private String executeChatCall(Prompt prompt) {
        try {
            return resilientAiClient.callChat(prompt).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error during AI chat call", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to get response from AI model", e);
        }
    }
}
