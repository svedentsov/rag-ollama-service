// src/main/java/com/example/ragollama/service/RagOrchestrationService.java
package com.example.ragollama.service;

import com.example.ragollama.dto.RagQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagOrchestrationService {

    private final ResilientOllamaClient resilientOllamaClient;
    private final PromptService promptService;
    private final Optional<RerankingService> rerankingService;
    private final ContextAssemblerService contextAssemblerService;
    private final VectorSearchService vectorSearchService;
    private final AsyncTaskExecutor taskExecutor;

    /**
     * Выполняет полный RAG-пайплайн и возвращает готовый ответ.
     *
     * @return CompletableFuture с финальным ответом.
     */
    public CompletableFuture<RagQueryResponse> execute(String query, int topK, double similarityThreshold) {
        CompletableFuture<List<Document>> documentsFuture = retrieveAndRerankDocuments(query, topK, similarityThreshold);
        return documentsFuture.thenComposeAsync(documents -> generateAnswer(documents, query), taskExecutor);
    }

    /**
     * Выполняет RAG-пайплайн и возвращает ответ в виде реактивного потока.
     *
     * @return {@link Flux} с частями ответа от LLM.
     */
    public Flux<String> executeStream(String query, int topK, double similarityThreshold) {
        // Этап извлечения документов остается асинхронным
        CompletableFuture<List<Document>> documentsFuture = retrieveAndRerankDocuments(query, topK, similarityThreshold);

        // ИСПРАВЛЕНИЕ: Используем Mono.fromFuture для конвертации CompletableFuture в реактивный тип (Mono).
        // Затем используем .flatMapMany для перехода от Mono<List<Document>> к Flux<String>
        return Mono.fromFuture(documentsFuture)
                .flatMapMany(documents -> generateAnswerStream(documents, query));
    }

    private CompletableFuture<List<Document>> retrieveAndRerankDocuments(String query, int topK, double similarityThreshold) {
        return CompletableFuture.supplyAsync(() -> {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build();
            List<Document> similarDocuments = vectorSearchService.search(searchRequest);
            return rerankingService
                    .map(service -> {
                        log.debug("Выполняется переранжирование для {} документов.", similarDocuments.size());
                        return service.rerank(similarDocuments, query);
                    })
                    .orElse(similarDocuments);
        }, taskExecutor);
    }

    private CompletableFuture<RagQueryResponse> generateAnswer(List<Document> documents, String query) {
        String context = contextAssemblerService.assembleContext(documents);

        if (context.isEmpty()) {
            log.warn("Контекст пуст после сборки для запроса: '{}'", query);
            String emptyContextAnswer = "Извините, я не смог найти релевантную информацию в базе знаний по вашему вопросу.";
            return CompletableFuture.completedFuture(new RagQueryResponse(emptyContextAnswer, List.of()));
        }

        String promptString = promptService.createRagPrompt(Map.of("context", context, "question", query));
        Prompt prompt = new Prompt(promptString);

        return resilientOllamaClient.callChat(prompt).thenApply(generatedAnswer -> {
            List<String> sourceCitations = documents.stream()
                    .map(doc -> (String) doc.getMetadata().getOrDefault("source", "Unknown"))
                    .distinct()
                    .toList();
            log.info("Сгенерирован ответ на запрос '{}'. Использованные источники: {}", query, sourceCitations);
            return new RagQueryResponse(generatedAnswer, sourceCitations);
        });
    }

    /**
     * Генерирует ответ от LLM в виде потока.
     */
    private Flux<String> generateAnswerStream(List<Document> documents, String query) {
        String context = contextAssemblerService.assembleContext(documents);

        if (context.isEmpty()) {
            log.warn("Контекст пуст для потокового запроса: '{}'", query);
            return Flux.just("Извините, я не смог найти релевантную информацию в базе знаний по вашему вопросу.");
        }

        String promptString = promptService.createRagPrompt(Map.of("context", context, "question", query));
        Prompt prompt = new Prompt(promptString);

        return resilientOllamaClient.streamChat(prompt);
    }
}
