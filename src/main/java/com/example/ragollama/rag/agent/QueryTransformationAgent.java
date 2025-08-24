package com.example.ragollama.rag.agent;

import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * AI-агент, отвечающий за управляемое извлечение сущностей из запроса.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class QueryTransformationAgent implements QueryEnhancementAgent {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;

    private record ExtractedQueryTerms(String language, List<String> keywords) {
    }

    @Override
    public Mono<List<String>> enhance(String originalQuery) {
        log.debug("QueryTransformationAgent: начало извлечения сущностей из запроса: '{}'", originalQuery);

        String promptString = promptService.render("queryTransformation", Map.of("query", originalQuery));

        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.FAST))
                .map(llmResponse -> {
                    try {
                        String cleanedJson = llmResponse.replaceAll("(?s)```json\\s*|\\s*```", "").trim();
                        ExtractedQueryTerms terms = objectMapper.readValue(cleanedJson, ExtractedQueryTerms.class);

                        if (!"russian".equalsIgnoreCase(terms.language())) {
                            log.warn("LLM определила язык как '{}'. Используется оригинальный запрос в качестве fallback.", terms.language());
                            return List.of(originalQuery);
                        }
                        if (terms.keywords() == null || terms.keywords().isEmpty()) {
                            log.warn("LLM не смогла извлечь ключевые слова. Используется оригинальный запрос.");
                            return List.of(originalQuery);
                        }
                        String transformedQuery = String.join(" ", terms.keywords());
                        log.info("QueryTransformationAgent: запрос '{}' успешно трансформирован в '{}'", originalQuery, transformedQuery);
                        return List.of(transformedQuery);
                    } catch (JsonProcessingException e) {
                        log.error("Не удалось распарсить JSON от LLM, используется оригинальный запрос. Ответ LLM: {}", llmResponse, e);
                        return List.of(originalQuery);
                    }
                });
    }
}
