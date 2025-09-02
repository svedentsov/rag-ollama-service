package com.example.ragollama.shared.llm;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * "Чистый" клиент-оркестратор для взаимодействия с LLM.
 * <p>
 * Эта версия реализует паттерн "Фасад" и отвечает исключительно за
 * оркестрацию вызова: выбор модели, вызов шлюза и применение политик
 * отказоустойчивости через исполнитель. Он не содержит никакой
 * низкоуровневой логики.
 */
@Component
@RequiredArgsConstructor
public class LlmClient {

    private final LlmGateway llmGateway;
    private final LlmRouterService llmRouterService;
    private final ResilientLlmExecutor resilientExecutor;

    /**
     * Асинхронно вызывает чат-модель для получения полного, не-потокового ответа.
     *
     * @param prompt     Промпт для отправки в модель.
     * @param capability Требуемый уровень возможностей модели.
     * @return {@link CompletableFuture}, который будет завершен строковым ответом от LLM.
     */
    public CompletableFuture<String> callChat(Prompt prompt, ModelCapability capability) {
        return resilientExecutor.execute(() -> {
            OllamaOptions options = buildOptions(capability);
            return llmGateway.call(prompt, options);
        }).toFuture();
    }

    /**
     * Вызывает чат-модель для получения ответа в виде непрерывного потока токенов.
     *
     * @param prompt     Промпт для отправки в модель.
     * @param capability Требуемый уровень возможностей модели.
     * @return {@link Flux}, который асинхронно эмитит части ответа от LLM.
     */
    public Flux<String> streamChat(Prompt prompt, ModelCapability capability) {
        return resilientExecutor.executeStream(() -> {
            OllamaOptions options = buildOptions(capability);
            return llmGateway.stream(prompt, options);
        });
    }

    /**
     * Вспомогательный метод для построения объекта опций на основе выбранной модели.
     *
     * @param capability Требуемый уровень возможностей.
     * @return Сконфигурированный объект {@link OllamaOptions}.
     */
    private OllamaOptions buildOptions(ModelCapability capability) {
        String modelName = llmRouterService.getModelFor(capability);
        return OllamaOptions.builder()
                .model(modelName)
                .build();
    }
}
