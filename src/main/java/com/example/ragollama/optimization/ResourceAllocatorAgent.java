package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.optimization.model.ResourceAllocationReport;
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
 * AI-агент, который анализирует исторические метрики и текущую конфигурацию
 * для выработки рекомендаций по оптимизации выделенных ресурсов.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceAllocatorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "resource-allocator-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует метрики и предлагает оптимальную конфигурацию ресурсов.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("historicalMetrics") &&
                context.payload().containsKey("currentConfigYaml");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        List<Map<String, Double>> metrics = (List<Map<String, Double>>) context.payload().get("historicalMetrics");
        String currentConfig = (String) context.payload().get("currentConfigYaml");

        try {
            String metricsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metrics);
            String promptString = promptService.render("resourceAllocatorPrompt", Map.of(
                    "metrics_json", metricsJson,
                    "current_config_yaml", currentConfig
            ));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.summary(),
                            Map.of("allocationReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации метрик.", e));
        }
    }

    /**
     * Безопасно парсит JSON-ответ от LLM в {@link ResourceAllocationReport}.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Десериализованный объект {@link ResourceAllocationReport}.
     * @throws ProcessingException если парсинг не удался.
     */
    private ResourceAllocationReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ResourceAllocationReport.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от ResourceAllocatorAgent: {}", jsonResponse, e);
            throw new ProcessingException("ResourceAllocatorAgent LLM вернул невалидный JSON.", e);
        }
    }
}
