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
 * AI-агент, реализующий стратегию Hypothetical Document Embeddings (HyDE).
 * Генерирует гипотетический ответ на вопрос для последующего семантического поиска.
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class HyDEAgent implements QueryEnhancementAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;

    @Override
    public Mono<List<String>> enhance(String originalQuery) {
        String promptString = promptService.render("hydePrompt", Map.of("query", originalQuery));
        Prompt prompt = new Prompt(promptString);
        return llmClient.callChat(prompt, ModelCapability.FASTEST)
                .map(hypotheticalDocument -> {
                    log.info("HyDE: сгенерирован гипотетический документ для запроса '{}'", originalQuery);
                    return List.of(hypotheticalDocument);
                });
    }
}
