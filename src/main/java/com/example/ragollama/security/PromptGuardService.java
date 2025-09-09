package com.example.ragollama.security;

import com.example.ragollama.shared.exception.PromptInjectionException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Сервис-страж, защищающий систему от атак типа Prompt Injection.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PromptGuardService {

    private final LlmClient llmClient;
    private final PromptService promptService;

    /**
     * Проверяет запрос пользователя на наличие признаков Prompt Injection.
     *
     * @param query Запрос пользователя.
     * @return Mono<Void>, который завершается успешно, если проверка пройдена,
     * или с ошибкой PromptInjectionException, если обнаружена атака.
     */
    public Mono<Void> validate(String query) {
        String promptString = promptService.render("promptGuardPrompt", Map.of("query", query));
        Prompt prompt = new Prompt(promptString);

        return Mono.fromFuture(llmClient.callChat(prompt, ModelCapability.FASTEST))
                .flatMap(response -> {
                    String classification = response.trim().toUpperCase();
                    if ("PROMPT_INJECTION_ATTEMPT".equals(classification)) {
                        log.warn("Обнаружена и заблокирована потенциальная атака Prompt Injection. Запрос: '{}'", query);
                        return Mono.error(new PromptInjectionException("Обнаружена потенциально вредоносная инструкция. Запрос отклонен."));
                    }
                    log.debug("Проверка безопасности для запроса пройдена успешно.");
                    return Mono.empty();
                });
    }
}
