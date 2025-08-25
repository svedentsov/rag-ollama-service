package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.QaAgent;
import com.example.ragollama.qaagent.model.CustomerImpactReport;
import com.example.ragollama.qaagent.tools.GitApiClient;
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
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, который анализирует изменения в коде и их историю для оценки
 * влияния на конечных пользователей.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerImpactAnalyzerAgent implements QaAgent {

    private final GitApiClient gitApiClient;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "customer-impact-analyzer";
    }

    @Override
    public String getDescription() {
        return "Анализирует diff кода и историю коммитов для оценки влияния на пользователей.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("changedFiles") &&
                context.payload().containsKey("oldRef") &&
                context.payload().containsKey("newRef");
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        List<String> changedFiles = (List<String>) context.payload().get("changedFiles");
        String oldRef = (String) context.payload().get("oldRef");
        String newRef = (String) context.payload().get("newRef");

        // Получаем diff и коммиты, затем передаем в LLM
        return gitApiClient.getDiff(oldRef, newRef)
                .flatMap(diff -> {
                    String promptString = promptService.render("customerImpactAnalyzer", Map.of(
                            "codeDiff", diff.isBlank() ? "Изменений в коде не найдено." : diff
                    ));
                    return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED));
                })
                .map(this::parseLlmResponse)
                .map(report -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Анализ влияния на пользователей завершен.",
                        Map.of("customerImpactReport", report)
                ))
                .toFuture();
    }

    private CustomerImpactReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, CustomerImpactReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("LLM вернула невалидный JSON для отчета о влиянии на пользователей.", e);
        }
    }
}
