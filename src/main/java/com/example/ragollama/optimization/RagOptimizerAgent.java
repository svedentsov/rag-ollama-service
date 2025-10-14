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
import reactor.core.publisher.Mono;

import java.util.Map;

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
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "rag-optimizer-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует метрики производительности RAG и предлагает улучшения конфигурации.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        return Mono.fromFuture(aggregatorService.aggregatePerformanceData())
                .flatMap(snapshot -> {
                    try {
                        String snapshotJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot);
                        String promptString = promptService.render("ragOptimizerPrompt", Map.of("performance_snapshot_json", snapshotJson));

                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                                .map(tuple -> parseLlmResponse(tuple.getT1()))
                                .map(report -> new AgentResult(
                                        getName(),
                                        AgentResult.Status.SUCCESS,
                                        report.summary(),
                                        Map.of("optimizationReport", report)
                                ));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new ProcessingException("Ошибка сериализации отчета о производительности.", e));
                    }
                });
    }

    private OptimizationReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, OptimizationReport.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от RAG Optimizer LLM: {}", jsonResponse, e);
            throw new ProcessingException("RAG Optimizer LLM вернул невалидный JSON.", e);
        }
    }
}
