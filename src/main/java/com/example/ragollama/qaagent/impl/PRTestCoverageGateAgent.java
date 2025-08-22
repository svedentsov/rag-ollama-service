package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.QaAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Простой агент-гейт, который проверяет результаты работы TestPrioritizerAgent.
 */
@Slf4j
@Component
public class PRTestCoverageGateAgent implements QaAgent {

    @Override
    public String getName() {
        return "pr-test-coverage-gate";
    }

    @Override
    public String getDescription() {
        return "Проверяет, были ли найдены тесты для измененных файлов.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        // Этот агент зависит от результатов предыдущего, поэтому canHandle всегда true,
        // но он будет вызван только в рамках пайплайна.
        return true;
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            // Читаем результаты предыдущего агента из контекста
            List<String> prioritizedTests = (List<String>) context.payload()
                    .getOrDefault("prioritizedTests", List.of());

            if (prioritizedTests.isEmpty()) {
                String summary = "Внимание: не найдено тестов, напрямую связанных с изменениями. Рекомендуется ручная проверка.";
                return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("coverage", "missing"));
            } else {
                String summary = "Проверка покрытия пройдена: найдены релевантные тесты.";
                return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("coverage", "found"));
            }
        });
    }
}
