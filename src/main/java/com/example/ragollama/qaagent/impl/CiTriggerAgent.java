package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.tools.CiApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, выполняющий "действие" - запуск задачи в CI/CD системе.
 * <p>
 * Этот агент является финальным шагом во многих автоматизированных конвейерах.
 * По умолчанию, его выполнение требует утверждения человеком.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CiTriggerAgent implements ToolAgent {

    private final CiApiClient ciApiClient;

    @Override
    public String getName() {
        return "ci-trigger";
    }

    @Override
    public String getDescription() {
        return "Запускает задачу в CI/CD системе (например, Jenkins, GitHub Actions) с заданными параметрами.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        // Ожидает, что в контексте будет как минимум имя CI-задачи.
        return context.payload().containsKey("jobName");
    }

    /**
     * По умолчанию, запуск внешних задач является рискованной операцией
     * и требует утверждения человеком.
     *
     * @return {@code true}
     */
    @Override
    public boolean requiresApproval() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String jobName = (String) context.payload().get("jobName");
        Map<String, Object> parameters = (Map<String, Object>) context.payload()
                .getOrDefault("parameters", Map.of());

        log.info("CiTriggerAgent: инициирован запуск CI-задачи '{}' с параметрами: {}", jobName, parameters);

        return ciApiClient.triggerJob(jobName, parameters)
                .map(response -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Задача '" + jobName + "' успешно запущена в CI/CD. Ответ системы: " + response,
                        Map.of("ciResponse", response)
                ))
                .toFuture();
    }
}
