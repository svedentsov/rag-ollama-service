package com.example.ragollama.agent.ci.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.Toolbox;
import com.example.ragollama.agent.ci.tool.CiApiClient;
import com.example.ragollama.agent.config.CiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент-инструмент, инкапсулирующий логику запуска задачи отката релиза.
 * <p>
 * Этот агент является критически важным "исполнителем" для Incident Commander.
 * Его единственная ответственность — принять команду и делегировать ее
 * CI/CD системе через {@link CiApiClient}.
 * <p>
 * **Важно:** выполнение этого агента требует явного утверждения человеком,
 * что предотвращает случайные или ошибочные откаты.
 */
@Slf4j
@Component
@Toolbox(name = "DeploymentTools", description = "Инструменты для управления развертываниями (откат, продвижение).")
@RequiredArgsConstructor
public class DeploymentRollbackAgent implements ToolAgent {

    private final CiApiClient ciApiClient;
    private final CiProperties ciProperties;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "deployment-rollback-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Запускает CI/CD задачу для отката последнего релиза на стабильную версию.";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Агент готов к работе, если в контексте есть `culpritCommitSha` для отката.
     *
     * @param context Контекст выполнения.
     * @return {@code true} если контекст содержит `culpritCommitSha`.
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("culpritCommitSha");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Откат релиза — это критически важная и потенциально разрушительная
     * операция. Она **всегда** должна требовать явного утверждения от
     * дежурного инженера.
     *
     * @return всегда {@code true}.
     */
    @Override
    public boolean requiresApproval() {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Запускает задачу отката в CI/CD системе, передавая ей в качестве
     * параметра SHA коммита, который предположительно вызвал инцидент.
     *
     * @param context Контекст, содержащий `culpritCommitSha`.
     * @return {@link CompletableFuture} с результатом запуска CI-задачи.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String culpritCommitSha = (String) context.payload().get("culpritCommitSha");
        String jobName = ciProperties.rollbackJobName();
        log.info("DeploymentRollbackAgent: инициирован запуск отката. Проблемный коммит: {}", culpritCommitSha);
        // Передаем параметры в CI/CD задачу
        Map<String, Object> parameters = Map.of("CULPRIT_COMMIT_SHA", culpritCommitSha);
        return ciApiClient.triggerJob(jobName, parameters)
                .map(response -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Задача отката '" + jobName + "' успешно запущена в CI/CD.",
                        Map.of("ciResponse", response)
                ))
                .toFuture();
    }
}