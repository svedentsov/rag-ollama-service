package com.example.ragollama.rag.agent;

import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * AI-агент, реализующий стратегию "Step-Back Prompting".
 * Генерирует более общий, концептуальный вопрос на основе исходного.
 */
@Slf4j
@Component
@Order(30)
@RequiredArgsConstructor
public class StepBackQueryAgent implements QueryEnhancementAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;

    @Override
    public Mono<List<String>> enhance(String originalQuery) {
        String promptString = promptService.render("stepBackPrompt", Map.of("query", originalQuery));
        Prompt prompt = new Prompt(promptString);

        return Mono.fromFuture(llmClient.callChat(prompt, ModelCapability.FAST))
                .map(stepBackQuery -> {
                    log.info("Step-Back: для запроса '{}' сгенерирован концептуальный запрос '{}'", originalQuery, stepBackQuery);
                    return List.of(stepBackQuery);
                });
    }
}
