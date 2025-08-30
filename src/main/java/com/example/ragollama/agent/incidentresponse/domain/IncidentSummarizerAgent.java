package com.example.ragollama.agent.incidentresponse.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.incidentresponse.model.IncidentReport;
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
 * Мета-агент, выступающий в роли "AI On-Call Engineer".
 * <p>
 * Агрегирует информацию о недавних изменениях и логи, чтобы
 * предоставить дежурному инженеру сводный отчет об инциденте.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentSummarizerAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "incident-summarizer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует данные об инциденте и формирует сводный отчет с гипотезой о причине.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("alertName") && context.payload().containsKey("changedFiles");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        try {
            String contextJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.payload());
            String promptString = promptService.render("incidentSummarizer", Map.of("incident_context_json", contextJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.summary(),
                            Map.of("incidentReport", report)
                    ));

        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации контекста инцидента", e));
        }
    }

    /**
     * Безопасно парсит JSON-ответ от LLM в {@link IncidentReport}.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Десериализованный объект {@link IncidentReport}.
     * @throws ProcessingException если парсинг не удался.
     */
    private IncidentReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, IncidentReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Incident Summarizer LLM вернул невалидный JSON.", e);
        }
    }
}
