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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI-агент, реализующий стратегию Multi-Query.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class MultiQueryGeneratorAgent implements QueryEnhancementAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;

    @Override
    public Mono<List<String>> enhance(String originalQuery) {
        String promptString = promptService.render("multiQuery", Map.of("query", originalQuery));
        Prompt prompt = new Prompt(promptString);

        return Mono.fromFuture(llmClient.callChat(prompt, ModelCapability.FAST))
                .map(this::parseToList)
                .map(generatedQueries -> {
                    List<String> uniqueGenerated = generatedQueries.stream()
                            .filter(q -> !q.equalsIgnoreCase(originalQuery))
                            .toList();
                    List<String> allQueries = new ArrayList<>();
                    allQueries.add(originalQuery);
                    allQueries.addAll(uniqueGenerated);
                    return allQueries;
                })
                .doOnSuccess(queries ->
                        log.info("MultiQueryGeneratorAgent: сгенерировано {} запросов для '{}'",
                                queries.size(), originalQuery)
                );
    }

    /**
     * Разбирает многострочный ответ от LLM в список строк.
     *
     * @param llmResponse Сырой ответ от LLM.
     * @return Список очищенных строк.
     */
    private List<String> parseToList(String llmResponse) {
        if (llmResponse == null || llmResponse.isBlank()) {
            return List.of();
        }
        return Arrays.stream(llmResponse.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }
}
