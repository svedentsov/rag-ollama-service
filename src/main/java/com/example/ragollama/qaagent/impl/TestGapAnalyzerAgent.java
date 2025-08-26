package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.tools.CodebaseMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * QA-агент, который анализирует разницу в коммитах (diff) для обнаружения
 * "пробелов в тестировании" — изменений в исходном коде, которые не были
 * сопровождены соответствующими изменениями в тестах.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestGapAnalyzerAgent implements ToolAgent {

    private final CodebaseMappingService codebaseMappingService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "test-gap-analyzer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует изменения в Git и находит файлы с исходным кодом, для которых не были обновлены тесты.";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Агент ожидает, что в контексте уже будет результат работы {@link GitInspectorAgent}.
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("changedFiles");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> changedFiles = (List<String>) context.payload().get("changedFiles");

            // Шаг 1: Разделяем все измененные файлы на исходный код и тесты
            List<String> changedSourceFiles = changedFiles.stream()
                    .filter(file -> file.contains("src/main/java"))
                    .toList();

            Set<String> changedTestFiles = changedFiles.stream()
                    .filter(file -> file.contains("src/test/java"))
                    .collect(Collectors.toSet());

            // Шаг 2: Находим "пробелы" - исходные файлы без соответствующих изменений в тестах
            List<String> gaps = changedSourceFiles.stream()
                    .flatMap(sourceFile -> codebaseMappingService.findTestForAppFile(sourceFile).stream())
                    .filter(expectedTestFile -> !changedTestFiles.contains(expectedTestFile))
                    .map(this::getOriginalSourceFileFromTestPath) // Возвращаем имя исходного файла для отчета
                    .distinct()
                    .toList();

            String summary;
            if (gaps.isEmpty()) {
                summary = "Анализ покрытия тестами пройден. Для всех измененных файлов найдены изменения в тестах.";
                log.info(summary);
            } else {
                summary = String.format("Внимание! Обнаружено %d потенциальных пробелов в тестовом покрытии.", gaps.size());
                log.warn("{}. Файлы: {}", summary, gaps);
            }

            return new AgentResult(
                    getName(),
                    AgentResult.Status.SUCCESS,
                    summary,
                    Map.of("untestedSourceFiles", gaps)
            );
        });
    }

    /**
     * Вспомогательный метод для обратного преобразования пути к тесту в путь к исходному файлу.
     */
    private String getOriginalSourceFileFromTestPath(String testFilePath) {
        return testFilePath
                .replace("src/test/java", "src/main/java")
                .replace("Test.java", ".java");
    }
}
