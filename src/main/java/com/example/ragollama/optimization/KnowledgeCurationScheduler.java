package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.curation.scheduler.enabled", havingValue = "true")
public class KnowledgeCurationScheduler {
    private final AgentOrchestratorService orchestratorService;

    @Scheduled(cron = "${app.curation.scheduler.cron}")
    public void runScheduledCuration() {
        log.info("Планировщик запускает фоновую задачу курирования базы знаний...");
        orchestratorService.invoke("knowledge-curation-pipeline", new AgentContext(Map.of()))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Задача курирования завершилась с ошибкой.", ex);
                    } else {
                        log.info("Задача курирования успешно завершена.");
                    }
                });
    }
}
