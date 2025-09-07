package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.optimization.model.OptimizationReport;
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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Мета-агент, выступающий в роли "AI MLOps Engineer".
 * <p>
 * Анализирует комплексные данные о производительности RAG-системы и
 * генерирует конкретные, действенные рекомендации по ее оптимизации.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagOptimizerAgent implements ToolAgent {

    private final PerformanceDataAggregatorService aggregatorService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "rag-optimizer-agent";
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Анализирует метрики производительности RAG и предлагает улучшения конфигурации.";
    }

    /** {@inheritDoc} */
    @Override
    public boolean canHandle(AgentContext context) {
        // Этот агент запускается без входных данных, он сам их собирает.
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        // Шаг 1: Асинхронно собрать все данные о производительности.
        return aggregatorService.aggregatePerformanceData()
                .thenCompose(snapshot -> {
                    // Шаг 2: Передать данные в LLM для анализа и генерации рекомендаций.
                    try {
                        String snapshotJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot);
                        String promptString = promptService.render("ragOptimizerPrompt", Map.of("performance_snapshot_json", snapshotJson));

                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                                .thenApply(this::parseLlmResponse)
                                .thenApply(report -> new AgentResult(
                                        getName(),
                                        AgentResult.Status.SUCCESS,
                                        report.summary(),
                                        Map.of("optimizationReport", report)
                                ));
                    } catch (JsonProcessingException e) {
                        return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации отчета о производительности.", e));
                    }
                });
    }

    /**
     * Безопасно парсит JSON-ответ от LLM в {@link OptimizationReport}.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Десериализованный объект {@link OptimizationReport}.
     * @throws ProcessingException если парсинг не удался.
     */
    private OptimizationReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, OptimizationReport.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от RAG Optimizer LLM: {}", jsonResponse, e);
            throw new ProcessingException("RAG Optimizer LLM вернул невалидный JSON.", e);
        }
    }
}
