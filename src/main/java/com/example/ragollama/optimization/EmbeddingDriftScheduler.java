package com.example.ragollama.optimization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.optimization.embedding-drift.scheduler.enabled", havingValue = "true")
public class EmbeddingDriftScheduler {

    private final EmbeddingDriftDetectorService driftDetectorService;

    @Scheduled(cron = "${app.optimization.embedding-drift.scheduler.cron}")
    public void runScheduledDriftDetection() {
        log.info("Планировщик запускает задачу обнаружения дрейфа эмбеддингов...");
        driftDetectorService.detectDrift();
    }
}
