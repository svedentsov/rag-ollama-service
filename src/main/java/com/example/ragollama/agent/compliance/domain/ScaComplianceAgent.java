package com.example.ragollama.agent.compliance.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.compliance.model.ScaReport;
import com.example.ragollama.agent.compliance.model.ScannedDependency;
import com.example.ragollama.agent.compliance.tool.DependencyScannerService;
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
 * AI-агент, который проверяет лицензии зависимостей на соответствие политике компании.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScaComplianceAgent implements ToolAgent {

    private final DependencyScannerService scannerService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "sca-compliance-agent";
    }

    @Override
    public String getDescription() {
        return "Проверяет лицензии зависимостей проекта на соответствие политике компании.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("buildFileContent") && context.payload().containsKey("licensePolicy");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String buildContent = (String) context.payload().get("buildFileContent");
        String licensePolicy = (String) context.payload().get("licensePolicy");

        // Шаг 1: Детерминированное сканирование для сбора фактов
        List<ScannedDependency> dependencies = scannerService.scan(buildContent);

        if (dependencies.isEmpty()) {
            return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Зависимости для анализа не найдены.", Map.of()));
        }

        // Шаг 2: Вызов LLM для экспертной оценки на основе политики
        try {
            String dependenciesJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dependencies);
            String promptString = promptService.render("scaComplianceAgentPrompt", Map.of(
                    "LICENSE_POLICY", licensePolicy,
                    "DEPENDENCY_REPORT_JSON", dependenciesJson
            ));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                    .map(tuple -> parseLlmResponse(tuple.getT1()))
                    .map(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.summary(),
                            Map.of("scaReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации отчета о зависимостях", e));
        }
    }

    private ScaReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ScaReport.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от SCA LLM: {}", jsonResponse, e);
            throw new ProcessingException("SCA LLM вернул невалидный JSON.", e);
        }
    }
}
