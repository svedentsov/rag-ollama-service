package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.optimization.model.TraceAnalysisReport;
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
 * Мета-агент (L4), выступающий в роли "AI Observability Director".
 * <p>
 * Анализирует "сырые" данные распределенной трассировки, находит
 * узкие места и генерирует отчет с выводами и рекомендациями.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ObservabilityDirectorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "observability-director-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует данные распределенной трассировки и находит узкие места.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("traceData");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        Object traceData = context.payload().get("traceData");

        try {
            String traceJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(traceData);
            String promptString = promptService.render("observabilityDirectorPrompt", Map.of("trace_json", traceJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.summary(),
                            Map.of("traceAnalysisReport", report)
                    ));

        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации данных трассировки.", e));
        }
    }

    /**
     * Безопасно парсит JSON-ответ от LLM в {@link TraceAnalysisReport}.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Десериализованный объект {@link TraceAnalysisReport}.
     * @throws ProcessingException если парсинг не удался.
     */
    private TraceAnalysisReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, TraceAnalysisReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("ObservabilityDirectorAgent LLM вернул невалидный JSON.", e);
        }
    }
}