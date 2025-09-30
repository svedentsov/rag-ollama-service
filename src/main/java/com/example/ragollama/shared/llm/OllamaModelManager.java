package com.example.ragollama.shared.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Сервис, отвечающий за мониторинг и управление состоянием доступных моделей в Ollama.
 * <p>
 * Этот компонент инкапсулирует логику взаимодействия с Ollama API для получения
 * списка загруженных моделей. Он работает в **проактивном** режиме, периодически
 * обновляя свой внутренний кэш по расписанию. Это позволяет остальным частям
 * системы мгновенно получать актуальную информацию о доступных моделях без
 * выполнения блокирующих сетевых вызовов в момент обработки запроса пользователя.
 */
@Service
@Slf4j
public class OllamaModelManager {
    private final WebClient.Builder webClientBuilder;
    private final String ollamaBaseUrl;

    public OllamaModelManager(WebClient.Builder webClientBuilder,
                              @Value("${spring.ai.ollama.base-url}") String ollamaBaseUrl) {
        this.webClientBuilder = webClientBuilder;
        this.ollamaBaseUrl = ollamaBaseUrl;
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
     * Получает список всех доступных (загруженных) моделей из Ollama.
     * <p>
     * Результат кэшируется в кэше с именем 'ollama_available_models'.
     * Кэш является синхронизированным, чтобы предотвратить множественные
     * одновременные запросы к Ollama API при старте или при истечении TTL.
     * Метод помечен как `public`, чтобы Spring AOP мог его перехватить.
     *
     * @return Множество имен доступных моделей без тега ':latest'.
     */
    @Cacheable(value = "ollama_available_models", sync = true)
    public Set<String> getAvailableModels() {
        log.info("Обновление кэша доступных моделей из Ollama...");
        try {
            WebClient client = webClientBuilder.baseUrl(this.ollamaBaseUrl).build();
            OllamaTagResponse response = client.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(OllamaTagResponse.class)
                    .block(); // .block() здесь допустим, так как метод вызывается из @Scheduled в отдельном потоке

            if (response == null || response.models() == null) {
                throw new IllegalStateException("Ollama API вернул пустой ответ.");
            }

            Set<String> models = response.models().stream()
                    .map(OllamaModel::name)
                    .map(name -> name.replace(":latest", ""))
                    .collect(Collectors.toSet());
            log.info("Доступные модели в Ollama: {}", models);
            return models;

        } catch (Exception e) {
            log.error("Не удалось получить список моделей из Ollama API. Проверьте, что сервис Ollama запущен и доступен по адресу '{}'. Возвращен пустой список.", this.ollamaBaseUrl, e);
            return Collections.emptySet();
        }
    }

    /**
     * Периодически запускает обновление кэша доступных моделей.
     * Расписание задается с фиксированной задержкой для обеспечения
     * актуальности данных без создания избыточной нагрузки.
     */
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void refreshAvailableModelsCache() {
        log.debug("Плановое обновление кэша моделей Ollama...");
        getAvailableModels();
    }
}
