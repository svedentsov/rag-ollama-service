package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.QaAgent;
import com.example.ragollama.qaagent.tools.CodebaseMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Агент, который анализирует git diff и предлагает приоритетный список тестов для запуска.
 * <p>
 * Этот агент не использует LLM. Он реализует простую, но эффективную бизнес-логику:
 * 1. Парсит `git diff`, чтобы найти измененные файлы.
 * 2. Для каждого файла из `src/main/java` ищет соответствующий тестовый файл.
 * 3. Возвращает список найденных тестов.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestPrioritizerAgent implements QaAgent {

    public static final String GIT_DIFF_CONTENT_KEY = "gitDiffContent";
    private final CodebaseMappingService codebaseMappingService;
    private final AsyncTaskExecutor applicationTaskExecutor;

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
        return "Анализирует git diff и приоритизирует тесты для запуска в CI/CD.";
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
        // Выполняем логику в нашем общем пуле потоков
        return CompletableFuture.supplyAsync(() -> {
            String diffContent = (String) context.payload().get(GIT_DIFF_CONTENT_KEY);
            List<String> changedFiles = parseChangedFiles(diffContent);

            List<String> prioritizedTests = changedFiles.stream()
                    .filter(file -> file.startsWith("src/main/"))
                    .map(codebaseMappingService::findTestForAppFile)
                    .flatMap(Optional::stream)
                    .distinct()
                    .toList();

            String summary = "Приоритизация завершена. Найдено " + prioritizedTests.size() + " релевантных тестов.";
            log.info(summary + " Тесты: {}", prioritizedTests);

            return new AgentResult(
                    getName(),
                    AgentResult.Status.SUCCESS,
                    summary,
                    Map.of("prioritizedTests", prioritizedTests, "changedAppFiles", changedFiles)
            );
        }, applicationTaskExecutor);
    }

    /**
     * Извлекает список измененных файлов из стандартного вывода `git diff --name-only`.
     *
     * @param diffContent Содержимое diff.
     * @return Список путей к файлам.
     */
    private List<String> parseChangedFiles(String diffContent) {
        if (diffContent == null || diffContent.isBlank()) {
            return List.of();
        }
        return Arrays.stream(diffContent.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }
}
