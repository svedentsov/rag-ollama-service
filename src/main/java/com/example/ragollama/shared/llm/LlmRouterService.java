package com.example.ragollama.shared.llm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Сервис-роутер, отвечающий за интеллектуальный выбор LLM для выполнения запроса.
 * <p>
 * Эта версия была отрефакторена для соответствия Принципу Единственной Ответственности.
 * Роутер больше не занимается проверкой доступности моделей; он делегирует эту
 * задачу специализированному сервису {@link OllamaModelManager}.
 * <p>
 * Его единственная задача — на основе требуемой "возможности" (`ModelCapability`)
 * выбрать основную модель из конфигурации и, в случае ее недоступности,
 * запросить резервную (fallback) модель.
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

        log.error("Критическая ошибка: ни основная модель '{}', ни fallback-модель '{}' не доступны в Ollama. Доступные модели: {}",
                primaryModel, fallbackModel, availableModels);
        throw new IllegalStateException("Нет доступных LLM для обработки запроса.");
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
