package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * QA-агент, который находит семантические дубликаты для заданного тест-кейса.
 * <p>
 * Этот агент является фасадом над {@link TestCaseDeduplicationService}. Он извлекает
 * данные из {@link AgentContext} и делегирует выполнение сложной логики поиска
 * и верификации специализированному сервису.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestCaseDeduplicatorAgent implements ToolAgent {

    private final TestCaseDeduplicationService deduplicationService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "test-case-deduplicator";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Находит семантически похожие (дублирующиеся) тест-кейсы в базе знаний.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("testCaseId") && context.payload().containsKey("testCaseContent");
    }

    /**
     * Асинхронно выполняет поиск дубликатов.
     *
     * @param context Контекст, содержащий ID и контент тест-кейса.
     * @return {@link Mono} с результатом, содержащим список найденных дубликатов.
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String testCaseId = (String) context.payload().get("testCaseId");
        String testCaseContent = (String) context.payload().get("testCaseContent");

        return deduplicationService.findDuplicates(testCaseId, testCaseContent)
                .map(duplicates -> {
                    String summary = String.format("Поиск дубликатов для '%s' завершен. Найдено %d подтвержденных дубликатов.",
                            testCaseId, duplicates.size());
                    log.info(summary);

                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            summary,
                            Map.of("duplicates", duplicates)
                    );
                });
    }
}
