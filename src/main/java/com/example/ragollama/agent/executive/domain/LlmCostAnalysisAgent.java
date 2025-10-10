package com.example.ragollama.agent.executive.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.config.QuotaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * Агент-аналитик, рассчитывающий финансовую стоимость использования LLM.
 * <p>
 * Этот агент является примером "инструмента", который выполняет детерминированную
 * бизнес-логику. Он выполняет прямой SQL-запрос к таблице `llm_usage_log` для
 * агрегации данных об использовании токенов и рассчитывает их стоимость на основе
 * тарифов, заданных в типобезопасной конфигурации {@link QuotaProperties}.
 * Все блокирующие I/O операции с базой данных выполняются на отдельном пуле потоков.
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
     * Асинхронно выполняет SQL-запрос и рассчитывает стоимость.
     *
     * @param context Контекст выполнения (не используется).
     * @return {@link Mono} с результатом, содержащим общую стоимость.
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        return Mono.fromCallable(() -> {
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
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
