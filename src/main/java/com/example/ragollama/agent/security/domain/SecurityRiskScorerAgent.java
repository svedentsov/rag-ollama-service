package com.example.ragollama.agent.security.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.git.tools.GitApiClient;
import com.example.ragollama.agent.security.model.SecurityRiskReport;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, который выполняет аудит безопасности и комплаенса для измененного кода.
 * <p>
 * Синтезирует информацию об изменениях в коде и правилах доступа, чтобы
 * выявить потенциальные уязвимости, такие как утечки PII, нарушения OWASP Top 10
 * и проблемы с контролем доступа.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityRiskScorerAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "security-risk-scorer";
    }

    @Override
    public String getDescription() {
        return "Проводит аудит измененного кода на предмет рисков безопасности и комплаенса (OWASP, GDPR).";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("changedFiles") &&
                context.payload().containsKey("newRef") &&
                context.payload().containsKey("extractedRules");
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        List<String> changedFiles = (List<String>) context.payload().get("changedFiles");
        String newRef = (String) context.payload().get("newRef");
        List<Map<String, String>> rbacRules = (List<Map<String, String>>) context.payload().get("extractedRules");

        // Асинхронно получаем контент всех измененных файлов
        return Flux.fromIterable(changedFiles)
                .filter(file -> file.endsWith(".java") && file.startsWith("src/main/java"))
                .flatMap(file -> gitApiClient.getFileContent(file, newRef).map(content -> Map.entry(file, content)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(filesContentMap -> {
                    if (filesContentMap.isEmpty()) {
                        return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Не найдено измененных Java-файлов для аудита.", Map.of()));
                    }
                    try {
                        String filesJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(filesContentMap);
                        String rbacJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rbacRules);

                        String promptString = promptService.render("securityRiskScorerPrompt", Map.of(
                                "changedCode", filesJson,
                                "rbacRules", rbacJson
                        ));
                        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED))
                                .map(this::parseLlmResponse)
                                .map(report -> new AgentResult(
                                        getName(),
                                        AgentResult.Status.SUCCESS,
                                        "Аудит безопасности завершен. Найдено проблем: " + report.findings().size(),
                                        Map.of("securityRiskReport", report)
                                ));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new ProcessingException("Ошибка сериализации данных для LLM-аудитора", e));
                    }
                })
                .toFuture();
    }

    private SecurityRiskReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, SecurityRiskReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("LLM вернула невалидный JSON для отчета о рисках безопасности.", e);
        }
    }
}
