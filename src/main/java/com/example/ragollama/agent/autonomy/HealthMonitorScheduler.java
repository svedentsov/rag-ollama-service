package com.example.ragollama.agent.autonomy;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Планировщик, отвечающий за периодический запуск проактивного
 * агента-монитора {@link AutonomousMaintenanceAgent}.
 * <p>
 * Этот компонент отделен от основной логики агента и занимается
 * исключительно запуском задачи по расписанию, заданному в {@code application.yml}.
 * Активируется свойством {@code app.health-monitor.scheduler.enabled=true}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.health-monitor.scheduler.enabled", havingValue = "true")
public class HealthMonitorScheduler {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает полный цикл автономного анализа и создания задач по расписанию.
     * Расписание настраивается в {@code application.yml} через свойство
     * {@code app.health-monitor.scheduler.cron}.
     */
    @Scheduled(cron = "${app.health-monitor.scheduler.cron}")
    public void runAutonomousHealthCheck() {
        log.info("Планировщик запускает автономный аудит здоровья проекта...");
        // Запускаем конвейер, который, в свою очередь, запустит AutonomousMaintenanceAgent
        orchestratorService.invokePipeline("health-monitor-pipeline", new AgentContext(Map.of("days", 30)))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Автономный аудит здоровья проекта завершился с ошибкой.", ex);
                    } else {
                        log.info("Автономный аудит здоровья проекта успешно завершен.");
                    }
                });
    }
}