package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsFetcherAgent implements ToolAgent {

    @Override
    public String getName() {
        return "metrics-fetcher-agent";
    }

    @Override
    public String getDescription() {
        return "Симулирует сбор исторических метрик (CPU, Memory) из системы мониторинга.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("serviceName");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        return Mono.fromCallable(() -> {
                    String serviceName = (String) context.payload().get("serviceName");
                    log.info("Сбор mock-метрик для сервиса '{}' за последние 7 дней...", serviceName);

                    Random rand = new Random();
                    List<Map<String, Double>> metrics = new ArrayList<>();
                    for (int i = 0; i < 24 * 7; i++) {
                        double cpuUsage = 100 + rand.nextDouble() * 400;
                        double memoryUsage = 512 + rand.nextDouble() * 512;
                        if (i % 24 == 14) {
                            cpuUsage *= 1.8;
                            memoryUsage *= 1.5;
                        }
                        metrics.add(Map.of("cpu_millicores", cpuUsage, "memory_mib", memoryUsage));
                    }

                    log.info("Собрано {} точек данных для анализа.", metrics.size());
                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Исторические метрики успешно собраны.",
                            Map.of("historicalMetrics", metrics)
                    );
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
