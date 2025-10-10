package com.example.ragollama.agent.architecture.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.architecture.model.ArchitecturalReviewReport;
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
 * Мета-агент, выступающий в роли "AI Architecture Governor".
 * <p>
 * Агрегирует отчеты от всех аналитических агентов (архитектура, качество тестов,
 * производительность, приватность) и синтезирует из них единый,
 * исчерпывающий отчет-ревью для Pull Request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArchitectureReviewSynthesizerAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "architecture-review-synthesizer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Агрегирует все отчеты по качеству в единый архитектурный вердикт.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        // Запускается как финальный агент в конвейере, если есть хоть какие-то данные для анализа
        return context.payload().containsKey("changedFiles");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        try {
            String analysisJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.payload());
            String promptString = promptService.render("architecturalReviewSynthesizerPrompt", Map.of("analysis_reports_json", analysisJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .map(this::parseLlmResponse)
                    .map(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.executiveSummary(),
                            Map.of("architecturalReviewReport", report)
                    ));

        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации отчетов для архитектурного ревью", e));
        }
    }

    /**
     * Безопасно парсит JSON-ответ от LLM в {@link ArchitecturalReviewReport}.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Десериализованный объект {@link ArchitecturalReviewReport}.
     * @throws ProcessingException если парсинг не удался.
     */
    private ArchitecturalReviewReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ArchitecturalReviewReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Architecture Review LLM вернул невалидный JSON.", e);
        }
    }
}
