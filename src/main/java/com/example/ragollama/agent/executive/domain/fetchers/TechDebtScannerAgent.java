package com.example.ragollama.agent.executive.domain.fetchers;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Агент-сборщик, имитирующий сканирование на предмет устаревших зависимостей.
 */
@Slf4j
@Component
public class TechDebtScannerAgent implements ToolAgent {
    @Override
    public String getName() {
        return "tech-debt-scanner";
    }

    @Override
    public String getDescription() {
        return "Сканирует зависимости на предмет устаревания.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        return Mono.fromCallable(() -> {
            log.info("Сбор mock-данных об устаревших зависимостях...");
            Map<String, String> techLagReport = Map.of(
                    "org.springframework.boot:spring-boot-starter", "Требует обновления с 3.3.1 до 3.4.0",
                    "com.fasterxml.jackson.core:jackson-databind", "Обнаружена уязвимость CVE-2025-12345 в текущей версии"
            );
            return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Анализ устаревших зависимостей завершен.", Map.of("techLagReport", techLagReport));
        });
    }
}
