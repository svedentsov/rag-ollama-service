package com.example.ragollama.agent.executive.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.config.QuotaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Реальный агент-аналитик для расчета стоимости использования LLM.
 *
 * <p>Этот агент выполняет SQL-запрос к таблице `llm_usage_log` для
 * агрегации данных об использовании токенов и рассчитывает их
 * стоимость на основе тарифов, заданных в {@link QuotaProperties}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmCostAnalysisAgent implements ToolAgent {
    private final JdbcTemplate jdbcTemplate;
    private final QuotaProperties quotaProperties;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "llm-cost-analyzer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Рассчитывает стоимость использования LLM на основе логов.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return true; // Агент не требует специфических входных данных
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Расчет стоимости использования LLM...");
            String sql = "SELECT SUM(prompt_tokens) as total_prompt, SUM(completion_tokens) as total_completion FROM llm_usage_log";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

            if (result.isEmpty() || result.getFirst().get("total_prompt") == null) {
                return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Данных об использовании LLM не найдено.", Map.of("llmCosts", 0.0));
            }

            long promptTokens = ((Number) result.getFirst().get("total_prompt")).longValue();
            long completionTokens = ((Number) result.getFirst().get("total_completion")).longValue();

            double inputCost = (promptTokens / 1000.0) * quotaProperties.costs().input();
            double outputCost = (completionTokens / 1000.0) * quotaProperties.costs().output();
            double totalCost = inputCost + outputCost;

            return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Стоимость LLM успешно рассчитана.", Map.of("llmCosts", totalCost));
        });
    }
}
