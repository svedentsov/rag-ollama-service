package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.optimization.model.ConsistencyReport;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Мета-агент, который анализирует доказательства, собранные
 * {@link ConsistencyCheckerAgent}, и выносит вердикт о наличии противоречий.
 * <p>Выступает в роли "AI-Аудитора", который сравнивает факты из разных
 * источников (документация, код, API) и формирует отчет о несоответствиях.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrossValidatorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "cross-validator-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Сравнивает доказательства из разных источников и находит противоречия.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("allEvidence");
    }

    /**
     * {@inheritDoc}
     * <p>Выполняет безопасное извлечение данных из контекста, проверяя их тип
     * перед использованием, чтобы предотвратить `ClassCastException`.
     *
     * @param context Контекст, который должен содержать `allEvidence` типа `Map`.
     * @return {@link CompletableFuture} с результатом анализа.
     * @throws ProcessingException если данные в контексте имеют неверный тип.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String claim = (String) context.payload().get("claim");
        Object evidenceObject = context.payload().get("allEvidence");

        // Безопасная проверка типа перед приведением
        if (!(evidenceObject instanceof Map)) {
            String errorMessage = "Ошибка контракта: CrossValidatorAgent ожидал Map в 'allEvidence', но получил " +
                    (evidenceObject == null ? "null" : evidenceObject.getClass().getName());
            log.error(errorMessage);
            return CompletableFuture.failedFuture(new ProcessingException(errorMessage));
        }

        // Подавляем предупреждение, так как мы уже проверили основной тип.
        // Проверить вложенные дженерики (List<String>) во время выполнения невозможно из-за стирания типов.
        @SuppressWarnings("unchecked")
        Map<String, List<String>> allEvidence = (Map<String, List<String>>) evidenceObject;

        try {
            String evidenceJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(allEvidence);
            String promptString = promptService.render("crossValidator", Map.of(
                    "claim", claim,
                    "all_evidence_json", evidenceJson
            ));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.summary(),
                            Map.of("consistencyReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации доказательств.", e));
        }
    }

    /**
     * Безопасно парсит JSON-ответ от LLM в {@link ConsistencyReport}.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Десериализованный объект {@link ConsistencyReport}.
     * @throws ProcessingException если парсинг не удался.
     */
    private ConsistencyReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ConsistencyReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("CrossValidatorAgent LLM вернул невалидный JSON.", e);
        }
    }
}
