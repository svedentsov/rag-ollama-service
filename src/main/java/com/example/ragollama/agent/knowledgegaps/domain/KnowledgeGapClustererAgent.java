package com.example.ragollama.agent.knowledgegaps.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.knowledgegaps.model.KnowledgeGapReport;
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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * AI-агент, который кластеризует запросы пользователей по темам и предлагает
 * темы для новой документации.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeGapClustererAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "knowledge-gap-clusterer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Кластеризует запросы, на которые не нашлось ответа, и предлагает темы для документации.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("gapQueries");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<AgentResult> execute(AgentContext context) {
        List<String> gapQueries = (List<String>) context.payload().get("gapQueries");
        if (gapQueries == null || gapQueries.isEmpty()) {
            return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Нет запросов для анализа.", Map.of()));
        }

        try {
            String queriesJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(gapQueries);
            String promptString = promptService.render("knowledgeGapAnalyzer", Map.of("gap_queries_json", queriesJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .map(this::parseLlmResponse)
                    .map(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.summary(),
                            Map.of("knowledgeGapReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации запросов для анализа.", e));
        }
    }

    private KnowledgeGapReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, KnowledgeGapReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Knowledge Gap Analyzer LLM вернул невалидный JSON.", e);
        }
    }
}
