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
 * Агент-сборщик, имитирующий получение метрик из Git.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitMetricsFetcherAgent implements ToolAgent {
    @Override
    public String getName() {
        return "git-metrics-fetcher";
    }

    @Override
    public String getDescription() {
        return "Собирает метрики по Pull Request'ам из Git.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Сбор mock-данных метрик Git...");
            Map<String, Object> gitMetrics = Map.of(
                    "avgPrSizeLines", 280,
                    "avgPrTimeToMergeHours", 18.5,
                    "reworkRate", "15%"
            );
            return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Метрики Git успешно собраны.", Map.of("gitMetrics", gitMetrics));
        });
    }
}
