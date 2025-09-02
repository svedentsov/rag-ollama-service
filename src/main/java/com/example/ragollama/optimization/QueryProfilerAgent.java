package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.optimization.model.QueryProfile;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, который выполняет быстрый семантический анализ (профилирование)
 * пользовательского запроса для определения оптимальной RAG-стратегии.
 * <p>
 * Он действует как "входной классификатор", который предоставляет метаданные
 * о запросе для вышестоящего оркестратора.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryProfilerAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "query-profiler-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует запрос и определяет его семантический профиль (тип, широта поиска).";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("query");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String query = (String) context.payload().get("query");
        String promptString = promptService.render("queryProfiler", Map.of("query", query));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.FAST)
                .thenApply(this::parseLlmResponse)
                .thenApply(profile -> {
                    log.info("Запрос '{}' профилирован как: {}", query, profile);
                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Запрос успешно профилирован.",
                            Map.of("queryProfile", profile)
                    );
                });
    }

    /**
     * Безопасно парсит JSON-ответ от LLM в {@link QueryProfile}.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Десериализованный объект {@link QueryProfile}.
     * @throws ProcessingException если парсинг не удался.
     */
    private QueryProfile parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, QueryProfile.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Query Profiler LLM вернул невалидный JSON.", e);
        }
    }
}
