package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Агент-инструмент (L2), имитирующий сбор данных из системы
 * распределенной трассировки (например, Jaeger, Zipkin).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TraceCollectorAgent implements ToolAgent {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "trace-collector-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Собирает (имитирует) данные распределенной трассировки для заданного requestId.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("requestId");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Генерирует правдоподобные, но случайные данные о выполнении
     * различных шагов RAG-конвейера.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            String requestId = (String) context.payload().get("requestId");
            log.info("Имитация сбора трейса для requestId: {}", requestId);

            // Mock-данные, имитирующие спаны OpenTelemetry/Jaeger
            List<Map<String, Object>> traceData = List.of(
                    Map.of("spanId", "span-1", "operationName", "QueryProcessingStep", "durationMillis", 150),
                    Map.of("spanId", "span-2", "operationName", "RetrievalStep", "durationMillis", 450),
                    Map.of("spanId", "span-3", "operationName", "RerankingStep", "durationMillis", 80),
                    Map.of("spanId", "span-4", "operationName", "GenerationStep", "durationMillis", 2500),
                    Map.of("spanId", "span-5", "operationName", "TrustScoringStep", "durationMillis", 950)
            );

            return new AgentResult(
                    getName(),
                    AgentResult.Status.SUCCESS,
                    "Данные трассировки успешно собраны.",
                    Map.of("traceData", traceData)
            );
        });
    }
}