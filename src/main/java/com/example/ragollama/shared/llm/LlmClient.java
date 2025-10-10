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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Клиент-оркестратор для взаимодействия с LLM, полностью адаптированный для Project Reactor.
 * <p>
 * Этот фасад инкапсулирует всю сквозную логику:
 * <ol>
 *     <li>Проверка квот на использование.</li>
 *     <li>Выбор оптимальной LLM-модели через роутер.</li>
 *     <li>Применение политик отказоустойчивости (Retry, Circuit Breaker).</li>
 *     <li>Выполнение вызова к LLM через низкоуровневый шлюз.</li>
 *     <li>Асинхронное логирование использования токенов для FinOps.</li>
 * </ol>
 * Он предоставляет два основных метода: для "запрос-ответ" (`callChat`) и для
 * потоковой передачи (`streamChat`), обеспечивая единый и надежный интерфейс
 * для всего приложения.
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
     * Выполняет асинхронный, не-потоковый вызов к LLM.
     *
     * @param prompt     Промпт для LLM.
     * @param capability Требуемый уровень возможностей модели.
     * @return {@link Mono}, который по завершении будет содержать текстовый ответ.
     */
    public Mono<String> callChat(Prompt prompt, ModelCapability capability) {
        return callChat(prompt, capability, false);
    }

    /**
     * Выполняет асинхронный, не-потоковый вызов к LLM с возможностью запросить JSON-ответ.
     *
     * @param prompt     Промпт для LLM.
     * @param capability Требуемый уровень возможностей модели.
     * @param isJson     {@code true}, если требуется ответ в формате JSON.
     * @return {@link Mono}, который по завершении будет содержать текстовый ответ.
     */
    public Mono<String> callChat(Prompt prompt, ModelCapability capability, boolean isJson) {
        String username = "anonymous_user"; // Заглушка до внедрения аутентификации
        int promptTokens = tokenizationService.countTokens(prompt.getContents());

        return quotaService.isQuotaExceeded(username, promptTokens)
                .flatMap(isExceeded -> {
                    if (isExceeded) {
                        return Mono.error(new QuotaExceededException("Месячный лимит токенов исчерпан."));
                    }
                    // Асинхронно получаем имя модели
                    return llmRouterService.getModelFor(capability)
                            .flatMap(modelName -> {
                                OllamaOptions options = buildOptions(modelName, isJson);

                                return resilientExecutor.execute(() -> llmGateway.call(prompt, options))
                                        .doOnSuccess(chatResponse -> {
                                            LlmResponse llmResponse = toLlmResponse(chatResponse);
                                            // Запускаем асинхронное сохранение лога и не ждем его завершения
                                            usageTracker.trackUsage(modelName, llmResponse).subscribe();
                                        })
                                        .map(chatResponse -> chatResponse.getResult().getOutput().getText());
                            });
                });
    }

    /**
     * Выполняет потоковый вызов к LLM, корректно обрабатывая проверку квот.
     * <p>
     * Эта исправленная версия использует идиоматичный для Project Reactor подход:
     * сначала асинхронно проверяется квота, и в зависимости от результата
     * `flatMapMany` переключается либо на поток с ошибкой (`Flux.error`), либо
     * на основной поток генерации ответа от LLM.
     *
     * @param prompt     Промпт для LLM.
     * @param capability Требуемый уровень возможностей модели.
     * @return {@link Flux}, который будет эмитить части ответа по мере их генерации.
     */
    public Flux<String> streamChat(Prompt prompt, ModelCapability capability) {
        String username = "anonymous_user";
        int promptTokens = tokenizationService.countTokens(prompt.getContents());

        return quotaService.isQuotaExceeded(username, promptTokens)
                .flatMapMany(isExceeded -> {
                    if (isExceeded) {
                        return Flux.error(new QuotaExceededException("Месячный лимит токенов исчерпан."));
                    }
                    // Асинхронно получаем имя модели и продолжаем цепочку
                    return llmRouterService.getModelFor(capability)
                            .flatMapMany(modelName -> {
                                OllamaOptions options = buildOptions(modelName, false);
                                return resilientExecutor.executeStream(() -> llmGateway.stream(prompt, options))
                                        .map(chatResponse -> chatResponse.getResult().getOutput().getText());
                            });
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
