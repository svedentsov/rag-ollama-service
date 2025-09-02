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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * Клиент-оркестратор для взаимодействия с LLM, возвращающий CompletableFuture.
 * Эта версия использует AsyncTaskExecutor для гарантированной передачи SecurityContext.
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
     *
     * @param prompt     Промпт для отправки в модель.
     * @param capability Требуемый уровень возможностей модели.
     * @return {@link CompletableFuture} с текстовым ответом от LLM.
     */
    public CompletableFuture<String> callChat(Prompt prompt, ModelCapability capability) {
        return CompletableFuture.supplyAsync(() -> {
            String username = getAuthenticatedUsername();
            int promptTokens = tokenizationService.countTokens(prompt.getContents());
            if (quotaService.isQuotaExceeded(username, promptTokens)) {
                throw new QuotaExceededException("Месячный лимит токенов исчерпан.");
            }
            return llmRouterService.getModelFor(capability);
        }, applicationTaskExecutor).thenCompose(modelName ->
                resilientExecutor.execute(() -> {
                            OllamaOptions options = buildOptions(modelName);
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system_user";
    }

    private OllamaOptions buildOptions(String modelName) {
        return OllamaOptions.builder().model(modelName).build();
    }

    private LlmResponse toLlmResponse(ChatResponse chatResponse) {
        return new LlmResponse(
                chatResponse.getResult().getOutput().getText(),
                chatResponse.getMetadata().getUsage(),
                chatResponse.getMetadata()
        );
    }
}
