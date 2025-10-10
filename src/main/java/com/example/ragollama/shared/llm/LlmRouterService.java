package com.example.ragollama.shared.llm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

/**
 * Сервис-роутер, отвечающий за интеллектуальный выбор LLM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmRouterService {

    private final LlmProperties llmProperties;
    private final OllamaModelManager ollamaModelManager;

    /**
     * Асинхронно выбирает наиболее подходящую и доступную модель.
     *
     * @param capability Требуемый уровень возможностей модели.
     * @return {@link Mono} с именем доступной модели.
     */
    public Mono<String> getModelFor(ModelCapability capability) {
        String primaryModel = llmProperties.getModels().get(capability);
        String fallbackModel = llmProperties.getFallbackModel();

        return ollamaModelManager.getAvailableModels()
                .flatMap(availableModels -> {
                    if (availableModels.contains(primaryModel)) {
                        log.trace("Основная модель '{}' для возможности '{}' доступна.", primaryModel, capability);
                        return Mono.just(primaryModel);
                    }

                    log.warn("Основная модель '{}' для '{}' недоступна. Попытка использовать fallback-модель '{}'.",
                            primaryModel, capability, fallbackModel);

                    if (availableModels.contains(fallbackModel)) {
                        return Mono.just(fallbackModel);
                    }

                    log.error("Критическая ошибка: ни основная модель '{}', ни fallback-модель '{}' не доступны в Ollama. Доступные модели: {}",
                            primaryModel, fallbackModel, availableModels);
                    return Mono.error(new IllegalStateException("Нет доступных LLM для обработки запроса."));
                });
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
