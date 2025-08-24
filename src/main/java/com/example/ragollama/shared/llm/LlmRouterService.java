package com.example.ragollama.shared.llm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис-роутер, отвечающий за интеллектуальный выбор LLM для выполнения запроса.
 * <p>
 * Реализует логику отказоустойчивости на уровне моделей: если основная модель,
 * настроенная для определенной возможности, недоступна, сервис автоматически
 * переключится на резервную (fallback) модель.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmRouterService {

    private final LlmProperties llmProperties;
    private final OllamaModelManager ollamaModelManager;

    /**
     * Выбирает наиболее подходящую и доступную модель для запроса.
     *
     * @param capability Требуемый уровень возможностей модели.
     * @return Имя доступной модели для использования в запросе.
     */
    public String getModelFor(ModelCapability capability) {
        String primaryModel = llmProperties.getModels().get(capability);
        Set<String> availableModels = ollamaModelManager.getAvailableModels();

        if (availableModels.contains(primaryModel)) {
            log.trace("Основная модель '{}' для возможности '{}' доступна.", primaryModel, capability);
            return primaryModel;
        }

        String fallbackModel = llmProperties.getFallbackModel();
        log.warn("Основная модель '{}' для '{}' недоступна. Попытка использовать fallback-модель '{}'.",
                primaryModel, capability, fallbackModel);

        if (availableModels.contains(fallbackModel)) {
            return fallbackModel;
        }

        log.error("Критическая ошибка: ни основная модель '{}', ни fallback-модель '{}' не доступны в Ollama.",
                primaryModel, fallbackModel);
        throw new IllegalStateException("Нет доступных LLM для обработки запроса.");
    }

    /**
     * Внутренний компонент для управления состоянием доступных моделей в Ollama.
     */
    @Service
    public static class OllamaModelManager {
        private final WebClient.Builder webClientBuilder;
        private final String ollamaBaseUrl;
        public OllamaModelManager(WebClient.Builder webClientBuilder,
                                  @Value("${spring.ai.ollama.base-url}") String ollamaBaseUrl) {
            this.webClientBuilder = webClientBuilder;
            this.ollamaBaseUrl = ollamaBaseUrl;
        }

        private record OllamaTagResponse(List<OllamaModel> models) {}
        private record OllamaModel(String name) {}

        /**
         * Получает список всех доступных (загруженных) моделей из Ollama.
         * Результат кэшируется.
         *
         * @return Множество имен доступных моделей.
         */
        @Cacheable(value = "ollama_available_models", sync = true)
        public Set<String> getAvailableModels() {
            log.info("Обновление списка доступных моделей из Ollama...");
            try {
                WebClient client = webClientBuilder.baseUrl(this.ollamaBaseUrl).build();
                OllamaTagResponse response = client.get()
                        .uri("/api/tags")
                        .retrieve()
                        .bodyToMono(OllamaTagResponse.class)
                        .block();

                if (response == null || response.models() == null) {
                    throw new IllegalStateException("Ollama API вернул пустой ответ.");
                }

                return response.models().stream()
                        .map(OllamaModel::name)
                        .map(name -> name.replace(":latest", ""))
                        .collect(Collectors.toSet());
            } catch (Exception e) {
                log.error("Не удалось получить список моделей из Ollama API. Проверьте, что сервис Ollama запущен и доступен по адресу '{}'.", this.ollamaBaseUrl, e);
                return Collections.emptySet();
            }
        }
    }

    /**
     * Типобезопасная конфигурация для маппинга возможностей на имена моделей.
     */
    @Getter
    @Setter
    @ConfigurationProperties(prefix = "app.llm")
    public static class LlmProperties {
        private String fallbackModel;
        private Map<ModelCapability, String> models;
    }
}
