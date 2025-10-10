package com.example.ragollama.shared.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис, отвечающий за мониторинг и управление состоянием доступных моделей в Ollama.
 */
@Service
@Slf4j
public class OllamaModelManager {
    private final WebClient webClient;
    private final Mono<Set<String>> cachedModels;

    /**
     * Конструктор, инициализирующий WebClient и настраивающий кэширующий Mono.
     *
     * @param webClientBuilder WebClient.Builder из общей конфигурации.
     * @param ollamaBaseUrl    URL-адрес сервиса Ollama.
     */
    public OllamaModelManager(WebClient.Builder webClientBuilder,
                              @Value("${spring.ai.ollama.base-url}") String ollamaBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(ollamaBaseUrl).build();
        // Создаем кэширующий Mono, который будет хранить результат в течение 5 минут.
        this.cachedModels = fetchAvailableModels()
                .cache(Duration.ofMinutes(5));
    }

    /**
     * DTO для десериализации корневого объекта ответа от Ollama /api/tags.
     *
     * @param models Список моделей.
     */
    private record OllamaTagResponse(List<OllamaModel> models) {
    }

    /**
     * DTO для десериализации объекта одной модели.
     *
     * @param name Имя модели (например, 'llama3:latest').
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OllamaModel(String name) {
    }

    /**
     * Возвращает кэшированный {@link Mono} со списком доступных моделей.
     *
     * @return {@link Mono}, который эммитит множество имен моделей.
     */
    public Mono<Set<String>> getAvailableModels() {
        return this.cachedModels;
    }

    /**
     * Выполняет реальный HTTP-запрос к Ollama API для получения списка моделей.
     *
     * @return {@link Mono} с результатом запроса.
     */
    private Mono<Set<String>> fetchAvailableModels() {
        log.info("Обновление кэша доступных моделей из Ollama...");
        return webClient.get()
                .uri("/api/tags")
                .retrieve()
                .bodyToMono(OllamaTagResponse.class)
                .map(response -> {
                    if (response == null || response.models() == null) {
                        return Collections.<String>emptySet();
                    }
                    Set<String> models = response.models().stream()
                            .map(OllamaModel::name)
                            .map(name -> name.replace(":latest", ""))
                            .collect(Collectors.toSet());
                    log.info("Доступные модели в Ollama: {}", models);
                    return models;
                })
                .onErrorResume(e -> {
                    log.error("Не удалось получить список моделей из Ollama API. Проверьте, что сервис Ollama запущен и доступен. Возвращен пустой список.", e);
                    return Mono.just(Collections.emptySet());
                });
    }
}
