package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Агент, который группирует (кластеризует) падения тестов по схожим стек-трейсам.
 * Не использует LLM, полагаясь на эвристики и регулярные выражения.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FailureClustererAgent implements ToolAgent {

    private static final String LOG_CONTENT_KEY = "logContent";
    // Простое регулярное выражение для извлечения первой значащей строки ошибки
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile("at .*(Exception|Error):.*");

    @Override
    public String getName() {
        return "failure-clusterer";
    }

    @Override
    public String getDescription() {
        return "Группирует падения тестов по схожим стек-трейсам в логах.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey(LOG_CONTENT_KEY);
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            String logContent = (String) context.payload().get(LOG_CONTENT_KEY);
            Map<String, Long> clusters = Arrays.stream(logContent.split("(?=\\n\\s*at )")) // Делим лог на стек-трейсы
                    .map(trace -> EXCEPTION_PATTERN.matcher(trace).results().findFirst().map(match -> match.group(0)).orElse("Unknown Error"))
                    .collect(Collectors.groupingBy(errorLine -> errorLine, Collectors.counting()));
            String summary = "Найдено " + clusters.size() + " уникальных кластеров ошибок.";
            return new AgentResult(
                    getName(),
                    AgentResult.Status.SUCCESS,
                    summary,
                    Map.of("clusters", clusters)
            );
        });
    }
}
