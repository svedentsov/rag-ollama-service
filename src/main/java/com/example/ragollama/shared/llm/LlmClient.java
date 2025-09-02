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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * "Чистый" клиент-оркестратор для взаимодействия с LLM с интегрированным контролем квот.
 * <p>
 * Эта версия включает в себя полный цикл FinOps & Governance:
 * 1.  **Pre-check:** Перед вызовом LLM проверяет, не превышена ли квота пользователя.
 * 2.  **Execution:** Вызывает LLM через отказоустойчивый исполнитель.
 * 3.  **Post-tracking:** После успешного ответа асинхронно записывает данные об использовании.
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

    /**
     * Асинхронно вызывает чат-модель, возвращая только текстовый ответ.
     *
     * @param prompt     Промпт для отправки в модель.
     * @param capability Требуемый уровень возможностей модели.
     * @return {@link CompletableFuture} с текстовым ответом от LLM.
     * @throws QuotaExceededException если лимит пользователя исчерпан.
     */
    public CompletableFuture<String> callChat(Prompt prompt, ModelCapability capability) {
        String username = getAuthenticatedUsername();
        int promptTokens = tokenizationService.countTokens(prompt.getContents());
        if (quotaService.isQuotaExceeded(username, promptTokens)) {
            return CompletableFuture.failedFuture(
                    new QuotaExceededException("Месячный лимит токенов исчерпан.")
            );
        }

        String modelName = llmRouterService.getModelFor(capability);
        return resilientExecutor.execute(() -> {
                    OllamaOptions options = buildOptions(modelName);
                    return llmGateway.call(prompt, options);
                })
                .doOnSuccess(chatResponse -> {
                    LlmResponse llmResponse = toLlmResponse(chatResponse);
                    usageTracker.trackUsage(modelName, llmResponse);
                })
                .map(chatResponse -> chatResponse.getResult().getOutput().getText())
                .toFuture();
    }

    /**
     * Вызывает потоковую чат-модель, возвращая поток текстовых фрагментов.
     *
     * @param prompt     Промпт для отправки в модель.
     * @param capability Требуемый уровень возможностей модели.
     * @return {@link Flux} с текстовыми частями ответа от LLM.
     * @throws QuotaExceededException если лимит пользователя исчерпан.
     */
    public Flux<String> streamChat(Prompt prompt, ModelCapability capability) {
        String username = getAuthenticatedUsername();
        int promptTokens = tokenizationService.countTokens(prompt.getContents());
        if (quotaService.isQuotaExceeded(username, promptTokens)) {
            return Flux.error(new QuotaExceededException("Месячный лимит токенов исчерпан."));
        }

        String modelName = llmRouterService.getModelFor(capability);
        return resilientExecutor.executeStream(() -> {
            OllamaOptions options = buildOptions(modelName);
            return llmGateway.stream(prompt, options);
        }).map(chatResponse -> chatResponse.getResult().getOutput().getText());
    }

    private String getAuthenticatedUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private OllamaOptions buildOptions(String modelName) {
        return OllamaOptions.builder()
                .model(modelName)
                .build();
    }

    private LlmResponse toLlmResponse(ChatResponse chatResponse) {
        return new LlmResponse(
                chatResponse.getResult().getOutput().getText(),
                chatResponse.getMetadata().getUsage(),
                chatResponse.getMetadata()
        );
    }
}
