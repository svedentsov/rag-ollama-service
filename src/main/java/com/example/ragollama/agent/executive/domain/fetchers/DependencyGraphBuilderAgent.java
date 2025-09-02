package com.example.ragollama.agent.executive.domain.fetchers;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Агент-инструмент, имитирующий парсинг файлов сборки для построения графа зависимостей.
 */
@Slf4j
@Component
public class DependencyGraphBuilderAgent implements ToolAgent {
    @Override
    public String getName() {
        return "dependency-graph-builder";
    }

    @Override
    public String getDescription() {
        return "Парсит файлы сборки и строит граф зависимостей.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Сбор mock-данных о графе зависимостей...");
            Map<String, Object> dependencyGraph = Map.of(
                    "rag-ollama-service", List.of("shared-utils", "notification-client"),
                    "notification-service", List.of("shared-utils"),
                    "payment-gateway", List.of("shared-utils", "notification-client")
            );
            return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Граф зависимостей построен.", Map.of("dependencyGraph", dependencyGraph));
        });
    }
}
