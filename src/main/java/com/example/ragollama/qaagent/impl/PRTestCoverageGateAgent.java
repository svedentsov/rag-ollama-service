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
 * Простой агент-гейт, который проверяет результаты работы {@link TestPrioritizerAgent}.
 * <p>
 * Этот агент является примером компонента, который работает не с исходными данными,
 * а с результатами предыдущего шага в конвейере, обогащая итоговый отчет.
 */
@Slf4j
@Component
public class PRTestCoverageGateAgent implements QaAgent {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "pr-test-coverage-gate";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Проверяет, были ли найдены тесты для измененных файлов в Pull Request.";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Этот агент зависит от результатов предыдущего агента, которые передаются
     * через {@link AgentContext}. Поэтому он всегда готов к выполнению в рамках
     * правильно настроенного конвейера.
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            // Читаем результаты предыдущего агента из обогащенного контекста
            @SuppressWarnings("unchecked")
            List<String> prioritizedTests = (List<String>) context.payload()
                    .getOrDefault("prioritizedTests", List.of());

            if (prioritizedTests.isEmpty()) {
                String summary = "Внимание: не найдено тестов, напрямую связанных с изменениями в PR. Рекомендуется ручная проверка покрытия.";
                log.warn("PR Test Coverage Gate: покрытие тестами не найдено.");
                return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("coverage", "missing"));
            } else {
                String summary = "Проверка покрытия пройдена: для измененных файлов найдены релевантные тесты.";
                log.info("PR Test Coverage Gate: покрытие тестами найдено ({} тестов).", prioritizedTests.size());
                return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("coverage", "found"));
            }
        });
    }
}
