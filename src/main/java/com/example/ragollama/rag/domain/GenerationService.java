package com.example.ragollama.rag.domain;

import com.example.ragollama.rag.api.dto.RagQueryResponse;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.domain.generation.NoContextStrategy;
import com.example.ragollama.shared.exception.GenerationException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис, отвечающий за этап генерации ответа в RAG-конвейере.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    private final LlmClient llmClient;
    private final NoContextStrategy noContextStrategy;

    /**
     * Асинхронно генерирует полный ответ.
     *
     * @param prompt    Промпт для LLM.
     * @param documents Документы, использованные в контексте.
     * @param sessionId Идентификатор сессии.
     * @return {@link CompletableFuture} с {@link RagQueryResponse}.
     */
    public CompletableFuture<RagQueryResponse> generate(Prompt prompt, List<Document> documents, UUID sessionId) {
        if (documents == null || documents.isEmpty()) {
            log.warn("На этап Generation не передано документов. Применяется стратегия '{}'.", noContextStrategy.getClass().getSimpleName());
            return noContextStrategy.handle(prompt, sessionId).toFuture();
        }
        return llmClient.callChat(prompt, ModelCapability.BALANCED)
                .thenApply(generatedAnswer -> {
                    List<String> sourceCitations = extractCitations(documents);
                    return new RagQueryResponse(generatedAnswer, sourceCitations, sessionId);
                })
                .exceptionally(ex -> {
                    log.error("Ошибка на этапе генерации ответа LLM", ex);
                    throw new GenerationException("Не удалось сгенерировать ответ от LLM.", ex);
                });
    }

    /**
     * Генерирует ответ в виде структурированного потока.
     *
     * @param prompt    Промпт для LLM.
     * @param documents Документы, использованные в контексте.
     * @param sessionId Идентификатор сессии.
     * @return {@link Flux} со {@link StreamingResponsePart}.
     */
    public Flux<StreamingResponsePart> generateStructuredStream(Prompt prompt, List<Document> documents, UUID sessionId) {
        if (documents == null || documents.isEmpty()) {
            log.warn("В потоковом запросе на этап Generation не передано документов.");
            return noContextStrategy.handle(prompt, sessionId)
                    .flatMapMany(response -> Flux.just(
                            new StreamingResponsePart.Content(response.answer()),
                            new StreamingResponsePart.Done("Завершено без контекста")
                    ));
        }

        Flux<StreamingResponsePart> contentStream = llmClient.streamChat(prompt, ModelCapability.BALANCED)
                .map(StreamingResponsePart.Content::new);

        Mono<StreamingResponsePart> sourcesPart = Mono.fromSupplier(() ->
                new StreamingResponsePart.Sources(extractCitations(documents)));

        Mono<StreamingResponsePart> doneWithSessionPart = Mono.just(new StreamingResponsePart.Done("Успешно завершено. SessionID: " + sessionId));
        return Flux.concat(contentStream, sourcesPart, doneWithSessionPart)
                .doOnError(ex -> log.error("Ошибка в потоке генерации ответа LLM", ex))
                .onErrorResume(ex -> {
                    String errorMessage = "Ошибка при генерации ответа: " + ex.getMessage();
                    return Flux.just(new StreamingResponsePart.Error(errorMessage));
                });
    }

    /**
     * Извлекает уникальные имена источников из метаданных документов.
     *
     * @param documents Список документов-источников.
     * @return Список уникальных имен источников.
     */
    private List<String> extractCitations(List<Document> documents) {
        return documents.stream()
                .map(doc -> (String) doc.getMetadata().getOrDefault("source", "Unknown"))
                .distinct()
                .toList();
    }
}
