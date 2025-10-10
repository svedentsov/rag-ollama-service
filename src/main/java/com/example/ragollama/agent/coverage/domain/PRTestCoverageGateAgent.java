package com.example.ragollama.agent.coverage.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Простой агент-гейт, который проверяет результаты работы {@link TestPrioritizerAgent}.
 * <p>
 * Этот агент является примером компонента, который работает не с исходными данными,
 * а с результатами предыдущего шага в конвейере, обогащая итоговый отчет.
 */
@Slf4j
@Component
public class PRTestCoverageGateAgent implements ToolAgent {

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
        return Mono.fromCallable(() -> {
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
