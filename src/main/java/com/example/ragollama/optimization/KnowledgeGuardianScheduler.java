package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Планировщик для периодического запуска агента-хранителя базы знаний.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.optimization.guardian.scheduler.enabled", havingValue = "true")
public class KnowledgeGuardianScheduler {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает полный цикл аудита консистентности базы знаний по расписанию.
     */
    @Scheduled(cron = "${app.optimization.guardian.scheduler.cron}")
    public void runScheduledConsistencyCheck() {
        log.info("Планировщик запускает фоновую задачу аудита консистентности базы знаний...");
        orchestratorService.invoke("knowledge-guardian-pipeline", new AgentContext(Map.of()))
                .subscribe(
                        result -> log.info("Задача аудита консистентности успешно завершена."),
                        ex -> log.error("Задача аудита консистентности завершилась с ошибкой.", ex)
                );
    }
}
