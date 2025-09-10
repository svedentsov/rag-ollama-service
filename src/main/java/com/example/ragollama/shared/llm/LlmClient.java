package com.example.ragollama.shared.llm;

import com.example.ragollama.agent.finops.domain.LlmUsageTracker;
import com.example.ragollama.agent.finops.domain.QuotaService;
import com.example.ragollama.shared.exception.QuotaExceededException;
import com.example.ragollama.shared.llm.model.LlmResponse;
import com.example.ragollama.shared.tokenization.TokenizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * Клиент-оркестратор для взаимодействия с LLM, возвращающий CompletableFuture.
 * <p> Этот класс является единой точкой входа (фасадом) для всего приложения при
 * работе с языковыми моделями. Он инкапсулирует и оркестрирует сквозные
 * бизнес-процессы, такие как:
 * <ul>
 *   <li>Проверка квот на использование.</li>
 *   <li>Интеллектуальный выбор модели через {@link LlmRouterService}.</li>
 *   <li>Выполнение вызова с применением политик отказоустойчивости через {@link ResilientLlmExecutor}.</li>
 *   <li>Логирование использования токенов для FinOps-аналитики.</li>
 * </ul>
 * Такая архитектура полностью изолирует бизнес-логику от сложностей взаимодействия с LLM.
 */
@Component
@RequiredArgsConstructor
public class LlmClient {

    private final LlmGateway llmGateway;
    private final LlmRouterService llmRouterService;
    private final ResilientLlmExecutor resilientExecutor;
    private final QuotaService quotaService;
    private final LlmUsageTracker usageTracker;
    private final TokenizationService tokenizationService;
    private final AsyncTaskExecutor applicationTaskExecutor;

    /**
     * Асинхронно вызывает чат-модель, возвращая текстовый ответ.
     * По умолчанию вызывает модель в стандартном текстовом режиме.
     *
     * @param prompt     Промпт для отправки в модель.
     * @param capability Требуемый уровень возможностей модели.
     * @return {@link CompletableFuture} с текстовым ответом от LLM.
     */
    public CompletableFuture<String> callChat(Prompt prompt, ModelCapability capability) {
        return callChat(prompt, capability, false);
    }

    /**
     * Асинхронно вызывает чат-модель с возможностью указания JSON-режима.
     *
     * @param prompt     Промпт для отправки в модель.
     * @param capability Требуемый уровень возможностей модели.
     * @param isJson     Если true, модель будет инструктирована вернуть валидный JSON.
     * @return {@link CompletableFuture} с текстовым ответом от LLM.
     */
    public CompletableFuture<String> callChat(Prompt prompt, ModelCapability capability, boolean isJson) {
        return CompletableFuture.supplyAsync(() -> {
            String username = "anonymous_user";
            int promptTokens = tokenizationService.countTokens(prompt.getContents());
            if (quotaService.isQuotaExceeded(username, promptTokens)) {
                throw new QuotaExceededException("Месячный лимит токенов исчерпан.");
            }
            return llmRouterService.getModelFor(capability);
        }, applicationTaskExecutor).thenCompose(modelName ->
                resilientExecutor.execute(() -> {
                            OllamaOptions options = buildOptions(modelName, isJson);
                            return llmGateway.call(prompt, options);
                        }).toFuture()
                        .thenApply(chatResponse -> {
                            LlmResponse llmResponse = toLlmResponse(chatResponse);
                            usageTracker.trackUsage(modelName, llmResponse);
                            return llmResponse.content();
                        })
        );
    }

    /**
     * Вызывает потоковую чат-модель, возвращая поток текстовых фрагментов.
     *
     * @param prompt     Промпт для отправки в модель.
     * @param capability Требуемый уровень возможностей модели.
     * @return {@link Flux} с текстовыми частями ответа от LLM.
     */
    public Flux<String> streamChat(Prompt prompt, ModelCapability capability) {
        String username = "anonymous_user";
        int promptTokens = tokenizationService.countTokens(prompt.getContents());
        if (quotaService.isQuotaExceeded(username, promptTokens)) {
            return Flux.error(new QuotaExceededException("Месячный лимит токенов исчерпан."));
        }

        String modelName = llmRouterService.getModelFor(capability);
        return resilientExecutor.executeStream(() -> {
                    OllamaOptions options = buildOptions(modelName, false);
                    return llmGateway.stream(prompt, options);
                })
                .map(chatResponse -> chatResponse.getResult().getOutput().getText());
    }

    private OllamaOptions buildOptions(String modelName, boolean isJson) {
        OllamaOptions options = new OllamaOptions();
        options.setModel(modelName);
        if (isJson) {
            options.setFormat("json");
        }
        return options;
    }

    private LlmResponse toLlmResponse(ChatResponse chatResponse) {
        return new LlmResponse(
                chatResponse.getResult().getOutput().getText(),
                chatResponse.getMetadata().getUsage(),
                chatResponse.getMetadata()
        );
    }
}
