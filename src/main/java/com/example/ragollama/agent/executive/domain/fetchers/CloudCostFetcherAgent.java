package com.example.ragollama.agent.executive.domain.fetchers;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Агент-сборщик, имитирующий получение данных о затратах на инфраструктуру.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloudCostFetcherAgent implements ToolAgent {
    @Override
    public String getName() {
        return "cloud-cost-fetcher";
    }

    @Override
    public String getDescription() {
        return "Собирает данные о затратах на облачную инфраструктуру.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Сбор mock-данных о затратах на инфраструктуру...");
            Map<String, Double> cloudCosts = Map.of(
                    "rag-ollama-service-compute", 1250.75,
                    "postgres-db-instance", 450.20,
                    "neo4j-db-instance", 300.00,
                    "rabbitmq-cluster", 150.50
            );
            return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Данные о затратах на инфраструктуру собраны.", Map.of("cloudCosts", cloudCosts));
        });
    }
}
