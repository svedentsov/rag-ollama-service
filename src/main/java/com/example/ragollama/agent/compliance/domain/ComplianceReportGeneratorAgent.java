package com.example.ragollama.agent.compliance.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Финальный агент в конвейере сбора доказательств для аудита.
 * <p>
 * Этот мета-агент агрегирует все "улики", собранные предыдущими агентами,
 * и использует LLM для генерации единого, человекочитаемого отчета в формате Markdown,
 * который можно предоставить аудитору.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComplianceReportGeneratorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "compliance-report-generator";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Агрегирует все собранные доказательства и генерирует финальный отчет для аудита.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        // Запускается как финальный шаг, если есть хотя бы информация об измененных файлах
        return context.payload().containsKey("changedFiles");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        log.info("ComplianceReportGeneratorAgent: начало генерации финального аудиторского отчета...");
        try {
            // Создаем копию контекста, чтобы безопасно удалить большие поля
            Map<String, Object> contextForPrompt = new HashMap<>(context.payload());
            contextForPrompt.remove("applicationLogs");
            contextForPrompt.remove("jacocoReportContent"); // Если он там был

            String evidenceJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextForPrompt);
            String promptString = promptService.render("complianceReportGeneratorPrompt", Map.of("evidence_json", evidenceJson));
            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(markdownReport -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Отчет о соответствии для аудита успешно сгенерирован.",
                            Map.of("complianceReportMarkdown", markdownReport)
                    ));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации доказательств для отчета", e));
        }
    }
}
