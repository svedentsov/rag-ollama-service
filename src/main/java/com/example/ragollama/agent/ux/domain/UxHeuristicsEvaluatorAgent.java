package com.example.ragollama.agent.ux.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.ux.model.UxHeuristicsReport;
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

import java.util.Map;

/**
 * AI-агент, который оценивает HTML-код на соответствие 10 эвристикам
 * юзабилити Якоба Нильсена.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UxHeuristicsEvaluatorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "ux-heuristics-evaluator";
    }

    @Override
    public String getDescription() {
        return "Оценивает HTML-код на соответствие 10 эвристикам юзабилити Якоба Нильсена.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("htmlContent");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String htmlContent = (String) context.payload().get("htmlContent");

        String promptString = promptService.render("uxHeuristicsEvaluatorPrompt", Map.of("html_content", htmlContent));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                .map(tuple -> parseLlmResponse(tuple.getT1()))
                .map(report -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        report.summary(),
                        Map.of("uxHeuristicsReport", report)
                ));
    }

    private UxHeuristicsReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, UxHeuristicsReport.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от UX Evaluator LLM: {}", jsonResponse, e);
            throw new ProcessingException("UX Evaluator LLM вернул невалидный JSON.", e);
        }
    }
}
