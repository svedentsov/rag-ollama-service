package com.example.ragollama.shared.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
 * <p> Реализует логику отказоустойчивости на уровне моделей: если основная модель,
 * настроенная для определенной возможности, недоступна, сервис автоматически
 * переключится на резервную (fallback) модель. Это повышает общую надежность
 * системы, делая ее менее чувствительной к временной недоступности отдельных моделей в Ollama.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmRouterService {

    private final LlmProperties llmProperties;
    private final OllamaModelManager ollamaModelManager;

    /**
     * Выбирает наиболее подходящую и доступную модель для запроса.
     * <p> Метод сначала пытается использовать основную модель, настроенную для
     * требуемого уровня возможностей. Если она отсутствует в списке доступных
     * моделей Ollama, предпринимается попытка использовать резервную модель.
     * Если и она недоступна, выбрасывается исключение.
     *
     * @param capability Требуемый уровень возможностей модели (например, FAST, BALANCED).
     * @return Имя доступной модели для использования в запросе.
     * @throws IllegalStateException если ни одна подходящая модель не найдена.
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
     * <p> Этот класс инкапсулирует логику взаимодействия с Ollama API для получения
     * списка загруженных моделей. Результаты кэшируются для снижения нагрузки
     * и повышения производительности.
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

        /**
         * DTO для десериализации корневого объекта ответа от Ollama /api/tags.
         *
         * @param models Список моделей.
         */
        private record OllamaTagResponse(List<OllamaModel> models) {
        }

        /**
         * DTO для десериализации объекта одной модели.
         * <p>Аннотация {@code @JsonIgnoreProperties(ignoreUnknown = true)} делает
         * парсинг устойчивым к будущим изменениям в Ollama API, позволяя
         * игнорировать новые, неизвестные нам поля.
         *
         * @param name Имя модели (например, 'llama3:latest').
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        private record OllamaModel(String name) {
        }

        /**
         * Получает список всех доступных (загруженных) моделей из Ollama.
         * <p> Результат кэшируется в кэше с именем 'ollama_available_models'.
         * Кэш является синхронизированным, чтобы предотвратить множественные
         * одновременные запросы к Ollama API при старте или при истечении TTL.
         *
         * @return Множество имен доступных моделей без тега ':latest'.
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
     * Загружает настройки из {@code application.yml} с префиксом {@code app.llm}.
     */
    @Getter
    @Setter
    @ConfigurationProperties(prefix = "app.llm")
    public static class LlmProperties {
        /**
         * Имя резервной модели, которая будет использоваться, если основная недоступна.
         */
        private String fallbackModel;
        /**
         * Карта, сопоставляющая абстрактные возможности с конкретными именами моделей.
         */
        private Map<ModelCapability, String> models;
    }
}
