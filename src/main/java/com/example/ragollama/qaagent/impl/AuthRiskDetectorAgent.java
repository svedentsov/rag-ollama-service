package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.QaAgent;
import com.example.ragollama.qaagent.model.AuthRisk;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, который анализирует извлеченные правила доступа на предмет
 * потенциальных рисков безопасности.
 * <p>
 * Этот агент является вторым шагом в конвейере аудита безопасности,
 * принимая на вход результаты работы {@link RbacExtractorAgent}. Он использует
 * LLM как "эксперта по безопасности" для выявления нарушений
 * принципа наименьших привилегий и других распространенных уязвимостей.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthRiskDetectorAgent implements QaAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "auth-risk-detector";
    }

    @Override
    public String getDescription() {
        return "Анализирует список правил RBAC/ACL и выявляет потенциальные риски безопасности.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        // Зависит от результатов предыдущего агента
        return context.payload().containsKey("extractedRules");
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        List<Map<String, String>> extractedRules = (List<Map<String, String>>) context.payload()
                .getOrDefault("extractedRules", Collections.emptyList());

        if (extractedRules.isEmpty()) {
            log.info("AuthRiskDetectorAgent: нет правил для анализа, пропуск.");
            return CompletableFuture.completedFuture(new AgentResult(
                    getName(),
                    AgentResult.Status.SUCCESS,
                    "Анализ рисков не проводился, так как не было найдено правил доступа.",
                    Map.of("risks", Collections.emptyList())
            ));
        }

        try {
            String rulesAsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(extractedRules);
            String promptString = promptService.render("authRiskDetector", Map.of("rulesAsJson", rulesAsJson));

            return llmClient.callChat(new Prompt(promptString))
                    .thenApply(this::parseLlmResponse)
                    .thenApply(risks -> {
                        String summary = String.format("Анализ рисков завершен. Найдено %d потенциальных проблем.", risks.size());
                        log.info(summary);
                        return new AgentResult(
                                getName(),
                                AgentResult.Status.SUCCESS,
                                summary,
                                Map.of("risks", risks)
                        );
                    });
        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать правила доступа в JSON", e);
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации правил RBAC.", e));
        }
    }

    /**
     * Безопасно парсит JSON-ответ от LLM в список объектов {@link AuthRisk}.
     */
    private List<AuthRisk> parseLlmResponse(String llmResponse) {
        try {
            String cleanedJson = llmResponse.replaceAll("(?s)```json\\s*|\\s*```", "").trim();
            if (cleanedJson.isEmpty() || cleanedJson.equals("[]")) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM для AuthRisk: {}", llmResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON для AuthRisk.", e);
        }
    }
}
