package com.example.ragollama.shared.llm;

import com.example.ragollama.shared.llm.model.LlmResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * Клиент-оркестратор для взаимодействия с LLM, полностью адаптированный для Project Reactor.
 * <p>
 * Этот фасад инкапсулирует логику выбора модели и применения политик отказоустойчивости.
 * Проверка квот и логирование использования вынесены в AOP-аспект для чистоты кода.
 */
@Component
public class LlmClient {

    private final LlmGateway llmGateway;
    private final LlmRouterService llmRouterService;
    private final ResilientLlmExecutor resilientExecutor;

    /**
     * Конструктор для внедрения зависимостей.
     *
     * @param llmGateway        Низкоуровневый шлюз для прямого вызова LLM.
     * @param llmRouterService  Сервис для выбора модели на основе требуемых возможностей.
     * @param resilientExecutor Декоратор, добавляющий политики отказоустойчивости (Retry, Circuit Breaker).
     */
    public LlmClient(
            LlmGateway llmGateway,
            LlmRouterService llmRouterService,
            ResilientLlmExecutor resilientExecutor
    ) {
        this.llmGateway = llmGateway;
        this.llmRouterService = llmRouterService;
        this.resilientExecutor = resilientExecutor;
    }


    /**
     * Выполняет асинхронный, не-потоковый вызов к LLM.
     *
     * @param prompt     Промпт для LLM.
     * @param capability Требуемый уровень возможностей модели.
     * @return {@link Mono}, который по завершении будет содержать кортеж (Tuple2) с текстовым ответом и самим промптом.
     */
    public Mono<Tuple2<String, Prompt>> callChat(Prompt prompt, ModelCapability capability) {
        return callChat(prompt, capability, false);
    }

    /**
     * Выполняет асинхронный, не-потоковый вызов к LLM с возможностью запросить JSON-ответ.
     *
     * @param prompt     Промпт для LLM.
     * @param capability Требуемый уровень возможностей модели.
     * @param isJson     {@code true}, если требуется ответ в формате JSON.
     * @return {@link Mono}, который по завершении будет содержать кортеж (Tuple2) с текстовым ответом и самим промптом.
     */
    public Mono<Tuple2<String, Prompt>> callChat(Prompt prompt, ModelCapability capability, boolean isJson) {
        return llmRouterService.getModelFor(capability)
                .flatMap(modelName -> {
                    OllamaOptions options = buildOptions(modelName, isJson);
                    return resilientExecutor.execute(() -> llmGateway.call(prompt, options))
                            .map(chatResponse -> Tuples.of(
                                    chatResponse.getResult().getOutput().getText(),
                                    prompt
                            ));
                });
    }

    /**
     * Выполняет потоковый вызов к LLM.
     *
     * @param prompt     Промпт для LLM.
     * @param capability Требуемый уровень возможностей модели.
     * @return {@link Flux}, который будет эмитить части ответа по мере их генерации.
     */
    public Flux<String> streamChat(Prompt prompt, ModelCapability capability) {
        return llmRouterService.getModelFor(capability)
                .flatMapMany(modelName -> {
                    OllamaOptions options = buildOptions(modelName, false);
                    return resilientExecutor.executeStream(() -> llmGateway.stream(prompt, options))
                            .map(chatResponse -> chatResponse.getResult().getOutput().getText());
                });
    }

    /**
     * Собирает объект {@link OllamaOptions} для вызова API.
     *
     * @param modelName Имя модели для использования.
     * @param isJson    Флаг, указывающий, нужно ли запрашивать JSON-формат.
     * @return Сконфигурированный объект опций.
     */
    private OllamaOptions buildOptions(String modelName, boolean isJson) {
        OllamaOptions options = new OllamaOptions();
        options.setModel(modelName);
        if (isJson) {
            options.setFormat("json");
        }
        return options;
    }

    /**
     * Преобразует низкоуровневый {@link ChatResponse} в наш унифицированный доменный объект.
     *
     * @param chatResponse Ответ от Spring AI.
     * @return Объект {@link LlmResponse}.
     */
    private LlmResponse toLlmResponse(ChatResponse chatResponse) {
        return new LlmResponse(
                chatResponse.getResult().getOutput().getText(),
                chatResponse.getMetadata().getUsage(),
                chatResponse.getMetadata()
        );
    }
}
