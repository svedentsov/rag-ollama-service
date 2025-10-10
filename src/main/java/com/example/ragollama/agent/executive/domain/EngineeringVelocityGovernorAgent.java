package com.example.ragollama.agent.executive.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.executive.model.EngineeringEfficiencyReport;
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
 * Мета-агент "AI VP of Engineering".
 * <p>
 * Синтезирует данные от различных сборщиков метрик (CI/CD, Git, Jira)
 * для формирования стратегического отчета об эффективности инженерных процессов.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EngineeringVelocityGovernorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "engineering-velocity-governor";
    }

    @Override
    public String getDescription() {
        return "Анализирует метрики SDLC и генерирует отчет об эффективности.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        // Запускается после всех сборщиков
        return context.payload().containsKey("doraMetrics") &&
                context.payload().containsKey("gitMetrics") &&
                context.payload().containsKey("jiraMetrics");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        try {
            String metricsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.payload());
            String promptString = promptService.render("engineeringVelocityGovernorPrompt", Map.of("metrics_json", metricsJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .map(this::parseLlmResponse)
                    .map(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.summary(),
                            Map.of("engineeringEfficiencyReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации инженерных метрик", e));
        }
    }

    private EngineeringEfficiencyReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, EngineeringEfficiencyReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Engineering Velocity Governor LLM вернул невалидный JSON.", e);
        }
    }
}
