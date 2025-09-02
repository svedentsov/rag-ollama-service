package com.example.ragollama.agent.executive.domain.fetchers;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Агент-сборщик, имитирующий получение DORA метрик из CI/CD системы.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CiCdMetricsFetcherAgent implements ToolAgent {
    @Override
    public String getName() {
        return "cicd-metrics-fetcher";
    }

    @Override
    public String getDescription() {
        return "Собирает DORA метрики из CI/CD системы.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Сбор mock-данных DORA метрик...");
            Map<String, Object> doraMetrics = Map.of(
                    "deploymentFrequency", "3.5 per week",
                    "leadTimeForChanges", "40 hours",
                    "meanTimeToRecovery", "2 hours",
                    "changeFailureRate", "12%"
            );
            return new AgentResult(getName(), AgentResult.Status.SUCCESS, "DORA метрики успешно собраны.", Map.of("doraMetrics", doraMetrics));
        });
    }
}
