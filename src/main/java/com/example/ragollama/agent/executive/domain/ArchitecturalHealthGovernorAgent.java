package com.example.ragollama.agent.executive.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.executive.model.ArchitecturalHealthReport;
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
 * Мета-агент "AI Chief Architect".
 * <p>
 * Синтезирует данные о структуре кода, зависимостях и технологическом стеке
 * для формирования долгосрочного плана по поддержанию здоровья архитектуры.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArchitecturalHealthGovernorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "architectural-health-governor";
    }

    @Override
    public String getDescription() {
        return "Анализирует архитектурные метрики и формирует технический роадмап.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("dependencyGraph"); // Запускается после всех сборщиков
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        try {
            String contextJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.payload());
            String promptString = promptService.render("architecturalHealthGovernor", Map.of("context_json", contextJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                    .map(tuple -> parseLlmResponse(tuple.getT1()))
                    .map(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.executiveSummary(),
                            Map.of("architecturalHealthReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации архитектурных данных.", e));
        }
    }

    private ArchitecturalHealthReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ArchitecturalHealthReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Architectural Health Governor LLM вернул невалидный JSON.", e);
        }
    }
}
