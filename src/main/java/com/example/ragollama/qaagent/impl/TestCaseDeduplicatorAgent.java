package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.domain.TestCaseDeduplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
     *
     * @return Уникальное имя агента.
     */
    @Override
    public String getName() {
        return "test-case-deduplicator";
    }

    /**
     * {@inheritDoc}
     *
     * @return Человекочитаемое описание назначения агента.
     */
    @Override
    public String getDescription() {
        return "Находит семантически похожие (дублирующиеся) тест-кейсы в базе знаний.";
    }

    /**
     * {@inheritDoc}
     *
     * @param context Контекст, который должен содержать 'testCaseId' и 'testCaseContent'.
     * @return {@code true}, если все необходимые ключи присутствуют.
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("testCaseId") && context.payload().containsKey("testCaseContent");
    }

    /**
     * Асинхронно выполняет поиск дубликатов.
     *
     * @param context Контекст, содержащий ID и контент тест-кейса.
     * @return {@link CompletableFuture} с результатом, содержащим список найденных дубликатов.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
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
                })
                .toFuture();
    }
}
