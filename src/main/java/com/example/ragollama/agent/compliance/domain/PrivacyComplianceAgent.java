package com.example.ragollama.agent.compliance.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.compliance.model.PrivacyReport;
import com.example.ragollama.agent.git.tools.GitApiClient;
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

/**
 * AI-агент, который проверяет измененный код на соответствие
 * политикам конфиденциальности и обработки персональных данных (PII).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrivacyComplianceAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "privacy-compliance-checker";
    }

    @Override
    public String getDescription() {
        return "Проверяет измененный код на соответствие политикам конфиденциальности (GDPR, CCPA).";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("privacyPolicy") &&
                context.payload().containsKey("changedFiles");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<AgentResult> execute(AgentContext context) {
        String policy = (String) context.payload().get("privacyPolicy");
        List<String> changedFiles = (List<String>) context.payload().get("changedFiles");
        String ref = (String) context.payload().getOrDefault("newRef", "main");

        return Flux.fromIterable(changedFiles)
                .filter(file -> file.endsWith(".java"))
                .flatMap(file -> gitApiClient.getFileContent(file, ref)
                        .map(content -> Map.entry(file, content))
                )
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(filesContentMap -> {
                    if (filesContentMap.isEmpty()) {
                        return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Не найдено Java-файлов для анализа.", Map.of()));
                    }
                    try {
                        String codeJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(filesContentMap);
                        String promptString = promptService.render("privacyComplianceCheckerPrompt", Map.of(
                                "privacy_policy", policy,
                                "changed_code_json", codeJson
                        ));
                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                                .map(this::parseLlmResponse)
                                .map(report -> new AgentResult(
                                        getName(),
                                        AgentResult.Status.SUCCESS,
                                        report.summary(),
                                        Map.of("privacyReport", report)
                                ));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new ProcessingException("Ошибка сериализации кода для Privacy-анализа", e));
                    }
                });
    }

    private PrivacyReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, PrivacyReport.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от Privacy Checker LLM: {}", jsonResponse, e);
            throw new ProcessingException("Privacy Checker LLM вернул невалидный JSON.", e);
        }
    }
}
