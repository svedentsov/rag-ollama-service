package com.example.ragollama.qaagent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис-оркестратор, управляющий выполнением **статических, жестко
 * заданных конвейеров** из QA-агентов.
 * <p>
 * Этот класс является простым и надежным исполнителем для часто используемых
 * сценариев, которые не требуют гибкости динамического AI-планировщика.
 * Он автоматически обнаруживает все доступные бины {@link ToolAgent} (инструменты)
 * при старте приложения, а затем компонует их в предопределенные
 * последовательности (конвейеры) в методе {@code definePipelines}.
 */
@Slf4j
@Service
public class AgentOrchestratorService {

    private final Map<String, QaAgent> agentMap;
    private final Map<String, List<QaAgent>> pipelines;

    /**
     * Конструктор, который автоматически обнаруживает все бины, реализующие
     * интерфейс {@link ToolAgent}, и инициализирует реестр агентов
     * и статических конвейеров.
     *
     * @param toolAgentsProvider Провайдер для ленивого получения списка агентов.
     */
    public AgentOrchestratorService(ObjectProvider<List<ToolAgent>> toolAgentsProvider) {
        List<ToolAgent> toolAgents = toolAgentsProvider.getObject();
        this.agentMap = toolAgents.stream()
                .collect(Collectors.toMap(QaAgent::getName, Function.identity()));
        this.pipelines = definePipelines();
        log.info("AgentOrchestratorService инициализирован. Доступные инструменты: {}. Статические конвейеры: {}",
                agentMap.keySet(), pipelines.keySet());
    }

    /**
     * Определяет все доступные статические конвейеры в системе.
     * <p>
     * Этот метод является центральным реестром всех многошаговых процессов.
     * Он компонует атомарных агентов в сложные цепочки для решения
     * конкретных бизнес-задач.
     *
     * @return Карта, где ключ - уникальное имя конвейера, а значение -
     * упорядоченный список агентов для выполнения.
     */
    private Map<String, List<QaAgent>> definePipelines() {
        return Map.ofEntries(
                // --- Single-Agent Pipelines (Tools) ---
                Map.entry("git-inspector-pipeline", List.of(agentMap.get("git-inspector"))),
                Map.entry("openapi-pipeline", List.of(agentMap.get("openapi-agent"))),
                Map.entry("flaky-test-detection-pipeline", List.of(agentMap.get("flaky-test-detector"))),
                Map.entry("spec-drift-sentinel-pipeline", List.of(agentMap.get("spec-drift-sentinel"))),
                Map.entry("github-pr-pipeline", List.of(agentMap.get("test-prioritizer"))),
                Map.entry("jira-bug-creation-pipeline", List.of(agentMap.get("bug-duplicate-detector"))),
                Map.entry("test-case-generation-pipeline", List.of(agentMap.get("test-case-generator"))),
                Map.entry("checklist-generation-pipeline", List.of(agentMap.get("checklist-generator"))),
                Map.entry("test-verifier-pipeline", List.of(agentMap.get("test-verifier"))),
                Map.entry("test-case-deduplication-pipeline", List.of(agentMap.get("test-case-deduplicator"))),
                Map.entry("contract-test-generation-pipeline", List.of(agentMap.get("contract-test-generator"))),
                Map.entry("accessibility-audit-pipeline", List.of(agentMap.get("accessibility-auditor"))),
                Map.entry("test-smell-refactoring-pipeline", List.of(agentMap.get("test-smell-refactorer"))),
                Map.entry("data-subset-masking-pipeline", List.of(agentMap.get("data-subset-masker"))),
                Map.entry("synthetic-data-generation-pipeline", List.of(agentMap.get("synthetic-data-builder"))),
                Map.entry("e2e-flow-synthesis-pipeline", List.of(agentMap.get("e2e-flow-synthesizer"))),
                Map.entry("canary-analysis-pipeline", List.of(agentMap.get("canary-analyzer"))),
                Map.entry("dp-synthetic-data-pipeline", List.of(agentMap.get("dp-synthetic-data-generator"))),
                Map.entry("xai-test-oracle-pipeline", List.of(agentMap.get("xai-test-oracle"))),
                Map.entry("arch-consistency-mapping-pipeline", List.of(agentMap.get("arch-consistency-mapper"))),
                Map.entry("sca-compliance-pipeline", List.of(agentMap.get("sca-compliance-agent"))),
                Map.entry("user-behavior-simulation-pipeline", List.of(agentMap.get("user-behavior-simulator"))),
                Map.entry("privacy-compliance-check-pipeline", List.of(agentMap.get("privacy-compliance-checker"))),
                Map.entry("test-mentor-pipeline", List.of(agentMap.get("test-mentor-bot"))),

                // --- Multi-Agent Composite Pipelines ---
                Map.entry("bug-reproduction-pipeline", List.of(
                        agentMap.get("bug-report-summarizer"),
                        agentMap.get("bug-repro-script-generator"))
                ),
                Map.entry("bug-pattern-detection-pipeline", List.of(agentMap.get("bug-pattern-detector"))),
                Map.entry("bug-report-analysis-pipeline", List.of(
                        agentMap.get("bug-report-summarizer"),
                        agentMap.get("bug-duplicate-detector"))
                ),
                Map.entry("jira-update-analysis-pipeline", List.of(
                        agentMap.get("jira-fetcher"),
                        agentMap.get("bug-duplicate-detector"))
                ),
                Map.entry("test-coverage-pipeline", List.of(
                        agentMap.get("git-inspector"),
                        agentMap.get("test-gap-analyzer"))
                ),
                Map.entry("security-audit-pipeline", List.of(
                        agentMap.get("git-inspector"),
                        agentMap.get("rbac-extractor"),
                        agentMap.get("security-risk-scorer")
                )),
                Map.entry("full-security-audit-pipeline", List.of(
                        agentMap.get("git-inspector"),
                        agentMap.get("sast-agent"),
                        agentMap.get("dast-test-generator"),
                        agentMap.get("security-log-analyzer"),
                        agentMap.get("security-report-aggregator")
                )),
                Map.entry("compliance-evidence-pipeline", List.of(
                        agentMap.get("git-inspector"),
                        agentMap.get("test-gap-analyzer"),
                        agentMap.get("sast-agent"),
                        agentMap.get("compliance-report-generator")
                )),
                Map.entry("root-cause-analysis-pipeline", List.of(
                        agentMap.get("flaky-test-detector"),
                        agentMap.get("git-inspector"),
                        agentMap.get("root-cause-analyzer"))
                ),
                Map.entry("coverage-audit-pipeline", List.of(
                        agentMap.get("git-inspector"),
                        agentMap.get("coverage-auditor")
                )),
                Map.entry("regression-prediction-pipeline", List.of(
                        agentMap.get("git-inspector"),
                        agentMap.get("coverage-auditor"),
                        agentMap.get("regression-predictor")
                )),
                Map.entry("customer-impact-analysis-pipeline", List.of(
                        agentMap.get("git-inspector"),
                        agentMap.get("customer-impact-analyzer")
                )),
                Map.entry("test-debt-report-pipeline", List.of(
                        agentMap.get("flakiness-tracker"),
                        agentMap.get("test-debt-analyzer")
                )),
                Map.entry("performance-analysis-pipeline", List.of(
                        agentMap.get("git-inspector"),
                        agentMap.get("performance-bottleneck-finder")
                )),
                Map.entry("knowledge-graph-update-pipeline", List.of(
                        agentMap.get("git-inspector"),
                        agentMap.get("code-parser"),
                        agentMap.get("code-graph-builder"),
                        agentMap.get("requirement-linker"),
                        agentMap.get("test-linker")
                )),
                Map.entry("agentic-pair-testing-pipeline", List.of(
                        agentMap.get("test-designer"),
                        agentMap.get("adversarial-tester"))
                ),
                Map.entry("rbac-fuzzing-pipeline", List.of(
                        agentMap.get("git-inspector"),
                        agentMap.get("rbac-extractor"),
                        agentMap.get("persona-generator"),
                        agentMap.get("fuzzing-test-generator")
                )),
                Map.entry("canary-decision-orchestration-pipeline", List.of(
                        agentMap.get("canary-analyzer"),
                        agentMap.get("canary-decision-orchestrator")
                )),
                Map.entry("feedback-to-test-pipeline", List.of(agentMap.get("feedback-to-test-converter")))
        );
    }

    /**
     * Предоставляет публичный доступ к списку имен всех
     * определенных статических конвейеров.
     *
     * @return Неизменяемое множество имен конвейеров.
     */
    public Set<String> getAvailablePipelines() {
        return Collections.unmodifiableSet(pipelines.keySet());
    }

    /**
     * Асинхронно запускает статический конвейер по его имени.
     * <p>
     * Метод последовательно выполняет каждого агента из определенного конвейера,
     * передавая и обогащая {@link AgentContext} на каждом шаге.
     *
     * @param pipelineName   Уникальное имя конвейера.
     * @param initialContext Начальный контекст с входными данными.
     * @return {@link CompletableFuture} с полным списком результатов от всех
     * агентов, выполненных в конвейере.
     */
    public CompletableFuture<List<AgentResult>> invokePipeline(String pipelineName, AgentContext initialContext) {
        List<QaAgent> agentsInPipeline = pipelines.get(pipelineName);
        if (agentsInPipeline == null || agentsInPipeline.isEmpty()) {
            log.warn("Конвейер с именем '{}' не найден или пуст.", pipelineName);
            return CompletableFuture.completedFuture(List.of());
        }

        log.info("Запуск статического конвейера '{}' с {} агентами.", pipelineName, agentsInPipeline.size());

        CompletableFuture<PipelineExecutionState> executionChain = CompletableFuture.completedFuture(
                new PipelineExecutionState(initialContext, List.of())
        );

        for (QaAgent agent : agentsInPipeline) {
            executionChain = executionChain.thenCompose(state -> {
                log.debug("Выполнение агента '{}' в конвейере '{}'", agent.getName(), pipelineName);
                if (!agent.canHandle(state.currentContext)) {
                    log.warn("Агент '{}' пропущен, так как не может обработать текущий контекст.", agent.getName());
                    return CompletableFuture.completedFuture(state);
                }
                return agent.execute(state.currentContext)
                        .thenApply(state::addResult);
            });
        }

        return executionChain.thenApply(PipelineExecutionState::getResults);
    }

    /**
     * Внутренний вспомогательный класс для хранения текущего состояния
     * выполнения конвейера.
     * <p>
     * Он является неизменяемым (immutable) и на каждом шаге создает новый
     * экземпляр с обновленным контекстом и списком результатов, что обеспечивает
     * потокобезопасность и предсказуемость.
     */
    private static class PipelineExecutionState {
        private final AgentContext currentContext;
        private final List<AgentResult> results;

        PipelineExecutionState(AgentContext currentContext, List<AgentResult> results) {
            this.currentContext = currentContext;
            this.results = new java.util.ArrayList<>(results);
        }

        /**
         * Добавляет новый результат и обогащает контекст для следующего шага.
         *
         * @param newResult Результат работы предыдущего агента.
         * @return Новый, обновленный экземпляр {@link PipelineExecutionState}.
         */
        public PipelineExecutionState addResult(AgentResult newResult) {
            List<AgentResult> newResults = new java.util.ArrayList<>(this.results);
            newResults.add(newResult);
            Map<String, Object> newPayload = new java.util.HashMap<>(this.currentContext.payload());
            newPayload.putAll(newResult.details());
            return new PipelineExecutionState(new AgentContext(newPayload), newResults);
        }

        public List<AgentResult> getResults() {
            return results;
        }
    }
}
