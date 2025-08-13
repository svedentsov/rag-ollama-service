package com.example.ragollama.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Отказоустойчивый клиент для взаимодействия с AI-сервисами (Ollama).
 * <p>
 * Этот компонент-обертка использует Resilience4j для добавления паттернов отказоустойчивости
 * (Circuit Breaker, Retry, TimeLimiter) к вызовам, которые могут быть нестабильными.
 * Все методы возвращают {@link CompletableFuture}, что необходимо для работы {@link TimeLimiter}.
 */
@Component
@RequiredArgsConstructor
public class ResilientAiClient {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final MetricService metricService;
    private static final String RESILIENCE_CONFIG_NAME = "ollamaApi";

    /**
     * Выполняет вызов чат-модели с применением паттернов отказоустойчивости.
     *
     * @param prompt Промпт для отправки в модель.
     * @return {@link CompletableFuture}, содержащий ответ модели в виде строки.
     */
    @CircuitBreaker(name = RESILIENCE_CONFIG_NAME)
    @TimeLimiter(name = RESILIENCE_CONFIG_NAME)
    @Retry(name = RESILIENCE_CONFIG_NAME)
    public CompletableFuture<String> callChat(Prompt prompt) {
        return CompletableFuture.supplyAsync(() ->
                metricService.recordTimer("llm.requests", () ->
                        chatClient.prompt(prompt).call().content()
                )
        );
    }

    /**
     * Выполняет поиск по схожести в векторном хранилище с применением паттернов отказоустойчивости.
     *
     * @param searchRequest Запрос на поиск.
     * @return {@link CompletableFuture}, содержащий список найденных документов.
     */
    @CircuitBreaker(name = RESILIENCE_CONFIG_NAME)
    @TimeLimiter(name = RESILIENCE_CONFIG_NAME)
    @Retry(name = RESILIENCE_CONFIG_NAME)
    public CompletableFuture<Object> similaritySearch(SearchRequest searchRequest) {
        return CompletableFuture.supplyAsync(() ->
                metricService.recordTimer("rag.retrieval", () ->
                        vectorStore.similaritySearch(searchRequest)
                )
        );
    }
}
