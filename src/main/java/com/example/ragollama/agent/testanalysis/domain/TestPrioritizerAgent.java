package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.rag.domain.TestCaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Интеллектуальный агент, который анализирует git diff и предлагает
 * приоритетный список тестов для запуска, используя семантический поиск.
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
    public Mono<AgentResult> execute(AgentContext context) {
        String diffContent = (String) context.payload().get(GIT_DIFF_CONTENT_KEY);
        if (diffContent == null || diffContent.isBlank()) {
            return Mono.just(new AgentResult(
                    getName(),
                    AgentResult.Status.SUCCESS,
                    "Изменения в коде не найдены, приоритизация тестов не требуется.",
                    Map.of("prioritizedTests", List.of())
            ));
        }

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
                });
    }
}
