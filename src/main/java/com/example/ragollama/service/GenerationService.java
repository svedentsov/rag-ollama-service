package com.example.ragollama.service;

import com.example.ragollama.dto.RagQueryResponse;
import com.example.ragollama.dto.StreamingResponsePart;
import com.example.ragollama.exception.GenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис, отвечающий исключительно за этап генерации ответа в RAG-конвейере.
 * Взаимодействует с LLM через отказоустойчивый {@link LlmClient} и формирует
 * финальный DTO-ответ, включая обработку случая отсутствия контекста.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    private final LlmClient llmClient;
    private static final String NO_CONTEXT_ANSWER = "Извините, я не смог найти релевантную информацию в базе знаний по вашему вопросу.";

    /**
     * Генерирует полный ответ от LLM асинхронно.
     *
     * @param prompt    Собранный и готовый к отправке промпт.
     * @param documents Список документов, использованных для контекста. Может быть пустым.
     * @return {@link CompletableFuture} с финальным {@link RagQueryResponse}.
     * @throws GenerationException в случае ошибки взаимодействия с LLM.
     */
    public CompletableFuture<RagQueryResponse> generate(Prompt prompt, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.warn("На этап Generation не передано документов. Возвращается ответ-заглушка.");
            return CompletableFuture.completedFuture(new RagQueryResponse(NO_CONTEXT_ANSWER, List.of()));
        }
        return llmClient.callChat(prompt)
                .thenApply(generatedAnswer -> {
                    List<String> sourceCitations = extractCitations(documents);
                    return new RagQueryResponse(generatedAnswer, sourceCitations);
                })
                .exceptionally(ex -> {
                    log.error("Ошибка на этапе генерации ответа LLM", ex);
                    throw new GenerationException("Не удалось сгенерировать ответ от LLM.", ex);
                });
    }

    /**
     * Генерирует ответ от LLM в виде структурированного потока (SSE).
     *
     * @param prompt    Собранный и готовый к отправке промпт.
     * @param documents Список документов-источников.
     * @return {@link Flux} со структурированными частями ответа {@link StreamingResponsePart}.
     */
    public Flux<StreamingResponsePart> generateStructuredStream(Prompt prompt, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.warn("В потоковом запросе на этап Generation не передано документов.");
            return Flux.just(
                    new StreamingResponsePart.Content(NO_CONTEXT_ANSWER),
                    new StreamingResponsePart.Done("Завершено без контекста")
            );
        }
        Flux<StreamingResponsePart> contentStream = llmClient.streamChat(prompt)
                .map(StreamingResponsePart.Content::new);
        Mono<StreamingResponsePart> sourcesPart = Mono.fromSupplier(() ->
                new StreamingResponsePart.Sources(extractCitations(documents)));
        Mono<StreamingResponsePart> donePart = Mono.just(new StreamingResponsePart.Done("Успешно завершено"));
        return Flux.concat(contentStream, sourcesPart, donePart)
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
