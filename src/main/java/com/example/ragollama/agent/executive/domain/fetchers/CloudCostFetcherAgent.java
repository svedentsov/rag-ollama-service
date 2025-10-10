package com.example.ragollama.agent.executive.domain.fetchers;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Агент-сборщик, имитирующий получение данных о затратах на инфраструктуру.
 *
 * <p>В реальной системе здесь была бы интеграция с API облачного провайдера
 * (AWS Cost Explorer, Azure Cost Management API и т.д.).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloudCostFetcherAgent implements ToolAgent {
    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "cloud-cost-fetcher";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Собирает данные о затратах на облачную инфраструктуру.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return true; // Агент не требует специфических входных данных
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Mono} с результатом, содержащим mock-данные о затратах.
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        return Mono.fromCallable(() -> {
            log.info("Сбор mock-данных о затратах на инфраструктуру...");
            // Имитация данных о месячных затратах в USD
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
