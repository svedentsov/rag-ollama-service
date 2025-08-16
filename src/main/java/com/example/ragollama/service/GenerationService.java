package com.example.ragollama.service;

import com.example.ragollama.dto.RagQueryResponse;
import com.example.ragollama.exception.GenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис, отвечающий исключительно за этап генерации ответа в RAG-конвейере.
 * Принимает на вход подготовленный промпт и список документов-источников.
 * Содержит логику для обработки случая, когда релевантные документы не найдены.
 * Взаимодействует с LLM через отказоустойчивый клиент и формирует финальный DTO-ответ.
 */
@Service
@Slf4j
public class GenerationService {

    private final ResilientOllamaClient resilientOllamaClient;
    private static final String NO_CONTEXT_ANSWER = "Извините, я не смог найти релевантную информацию в базе знаний по вашему вопросу.";

    /**
     * Конструктор для внедрения зависимостей.
     *
     * @param resilientOllamaClient Отказоустойчивый клиент для вызова LLM.
     */
    public GenerationService(ResilientOllamaClient resilientOllamaClient) {
        this.resilientOllamaClient = resilientOllamaClient;
    }

    /**
     * Генерирует полный ответ от LLM асинхронно.
     * *
     * Если список документов пуст, возвращает стандартный ответ-заглушку.
     * В противном случае вызывает LLM и формирует ответ с цитатами.
     *
     * @param prompt    Собранный и готовый к отправке промпт.
     * @param documents Список документов, использованных для контекста. Может быть пустым.
     * @return {@link CompletableFuture} с финальным {@link RagQueryResponse}.
     */
    public CompletableFuture<RagQueryResponse> generate(Prompt prompt, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.warn("На этап Generation не передано документов. Возвращается ответ-заглушка.");
            return CompletableFuture.completedFuture(new RagQueryResponse(NO_CONTEXT_ANSWER, List.of()));
        }

        return resilientOllamaClient.callChat(prompt)
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
     * Генерирует ответ от LLM в виде потока (stream).
     * *
     * Если список документов пуст, возвращает поток с одним элементом - стандартным ответом-заглушкой.
     * В противном случае, стримит ответ от LLM.
     *
     * @param prompt    Собранный и готовый к отправке промпт.
     * @param documents Список документов-источников. Может быть пустым.
     * @return {@link Flux} с частями (токенами) сгенерированного ответа.
     */
    public Flux<String> generateStream(Prompt prompt, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.warn("В потоковом запросе на этап Generation не передано документов. Возвращается ответ-заглушка.");
            return Flux.just(NO_CONTEXT_ANSWER);
        }

        return resilientOllamaClient.streamChat(prompt)
                .doOnError(ex -> log.error("Ошибка в потоке генерации ответа LLM", ex))
                .onErrorMap(ex -> new GenerationException("Ошибка в потоке генерации ответа.", ex));
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
