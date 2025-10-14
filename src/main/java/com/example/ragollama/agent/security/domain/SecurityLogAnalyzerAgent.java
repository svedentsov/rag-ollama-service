package com.example.ragollama.agent.security.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.security.model.SecurityFinding;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * QA-агент, выполняющий анализ логов на предмет аномалий безопасности (IAST-like).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityLogAnalyzerAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "security-log-analyzer";
    }

    @Override
    public String getDescription() {
        return "Анализирует логи приложения на предмет аномалий, указывающих на проблемы безопасности.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("applicationLogs");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String logs = (String) context.payload().get("applicationLogs");
        if (logs == null || logs.isBlank()) {
            return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Логи для анализа не предоставлены.", Map.of("logAnalysisFindings", List.of())));
        }

        String promptString = promptService.render("securityLogAnalyzerPrompt", Map.of("application_logs", logs));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                .map(tuple -> parseLlmResponse(tuple.getT1()))
                .map(findings -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Анализ логов завершен. Найдено аномалий: " + findings.size(),
                        Map.of("logAnalysisFindings", findings)
                ));
    }

    private List<SecurityFinding> parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от Log Analyzer LLM: {}", jsonResponse, e);
            throw new ProcessingException("Log Analyzer LLM вернул невалидный JSON.", e);
        }
    }
}
