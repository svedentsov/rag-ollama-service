package com.example.ragollama.agent.executive.domain.fetchers;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Агент-сборщик, имитирующий получение метрик жизненного цикла задач из Jira.
 */
@Slf4j
@Component
public class JiraMetricsFetcherAgent implements ToolAgent {
    @Override
    public String getName() {
        return "jira-metrics-fetcher";
    }

    @Override
    public String getDescription() {
        return "Собирает метрики жизненного цикла задач (Cycle Time) из Jira.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        return Mono.fromCallable(() -> {
            log.info("Сбор mock-данных о метриках Jira...");
            Map<String, Object> jiraEffort = Map.of(
                    "avgFeatureCycleTimeDays", 12.5,
                    "avgBugFixTimeHours", 8.0,
                    "workInProgressLimit", 25
            );
            return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Метрики из Jira успешно собраны.", Map.of("jiraMetrics", jiraEffort));
        });
    }
}
