package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.rag.domain.TestCaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Интеллектуальный агент, который анализирует git diff и предлагает
 * приоритетный список тестов для запуска, используя семантический поиск.
 * <p>
 * Этот агент больше не использует примитивное сопоставление имен. Вместо этого,
 * он делегирует поиск специализированному {@link TestCaseService}, который
 * выполняет RAG-поиск по базе тест-кейсов, используя содержимое
 * измененного кода в качестве поискового запроса.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestPrioritizerAgent implements ToolAgent {

    public static final String GIT_DIFF_CONTENT_KEY = "gitDiffContent";
    private final TestCaseService testCaseService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "test-prioritizer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует git diff и приоритизирует тесты для запуска в CI/CD с помощью семантического поиска.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey(GIT_DIFF_CONTENT_KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String diffContent = (String) context.payload().get(GIT_DIFF_CONTENT_KEY);
        if (diffContent == null || diffContent.isBlank()) {
            return CompletableFuture.completedFuture(new AgentResult(
                    getName(),
                    AgentResult.Status.SUCCESS,
                    "Изменения в коде не найдены, приоритизация тестов не требуется.",
                    Map.of("prioritizedTests", List.of())
            ));
        }

        // Используем наш RAG-сервис для поиска релевантных тест-кейсов
        return testCaseService.findRelevantTestCases(diffContent)
                .map(foundDocuments -> {
                    List<String> testNames = foundDocuments.stream()
                            .map(doc -> doc.getMetadata().get("source").toString())
                            .distinct()
                            .toList();

                    String summary = "Приоритизация завершена. Найдено " + testNames.size() + " релевантных тестов.";
                    log.info(summary + " Тесты: {}", testNames);

                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            summary,
                            Map.of("prioritizedTests", testNames)
                    );
                }).toFuture();
    }
}
