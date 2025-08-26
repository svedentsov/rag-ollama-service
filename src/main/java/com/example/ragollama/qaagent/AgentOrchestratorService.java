package com.example.ragollama.qaagent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис-оркестратор, управляющий выполнением **статических, жестко
 * заданных конвейеров** из QA-агентов.
 * <p>
 * Этот класс является простым и надежным исполнителем для часто используемых
 * сценариев, которые не требуют гибкости динамического AI-планировщика.
 * Он обнаруживает все доступные бины {@link ToolAgent} (инструменты) и компонует их
 * в предопределенные последовательности (конвейеры).
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
     * @param toolAgents Список всех реализаций {@link ToolAgent} (инструментов),
     *                   найденных Spring'ом.
     */
    public AgentOrchestratorService(List<ToolAgent> toolAgents) {
        this.agentMap = toolAgents.stream()
                .collect(Collectors.toMap(QaAgent::getName, Function.identity()));
        this.pipelines = definePipelines();
        log.info("AgentOrchestratorService инициализирован. Доступные инструменты: {}. Статические конвейеры: {}",
                agentMap.keySet(), pipelines.keySet());
    }

    /**
     * Определяет все доступные статические конвейеры в системе.
     * <p>
     * Каждый конвейер представляет собой упорядоченный список агентов,
     * которые выполняются последовательно, передавая обогащенный
     * контекст от одного к другому.
     *
     * @return Карта, где ключ - уникальное имя конвейера, а значение -
     * список агентов для выполнения.
     */
    private Map<String, List<QaAgent>> definePipelines() {
        return Map.ofEntries(
                Map.entry("git-inspector-pipeline", List.of(agentMap.get("git-inspector"))),
                Map.entry("openapi-pipeline", List.of(agentMap.get("openapi-agent"))),
                Map.entry("flaky-test-detection-pipeline", List.of(agentMap.get("flaky-test-detector"))),
                Map.entry("spec-drift-sentinel-pipeline", List.of(agentMap.get("spec-drift-sentinel"))),
                Map.entry("github-pr-pipeline", List.of(agentMap.get("test-prioritizer"))),
                Map.entry("jira-bug-creation-pipeline", List.of(agentMap.get("bug-duplicate-detector"))),
                Map.entry("test-case-generation-pipeline", List.of(agentMap.get("test-case-generator"))),
                Map.entry("test-verifier-pipeline", List.of(agentMap.get("test-verifier"))),
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
                Map.entry("release-readiness-pipeline", List.of(
                        agentMap.get("git-inspector"),
                        agentMap.get("coverage-auditor"),
                        agentMap.get("code-quality-impact-estimator"),
                        agentMap.get("flakiness-tracker"),
                        agentMap.get("release-readiness-assessor")
                )),
                Map.entry("risk-matrix-generation-pipeline", List.of(
                        agentMap.get("git-inspector"),
                        agentMap.get("customer-impact-analyzer"),
                        agentMap.get("code-quality-impact-estimator"),
                        agentMap.get("risk-matrix-generator")
                )),
                Map.entry("economic-impact-pipeline", List.of(agentMap.get("defect-economics-modeler")))
        );
    }

    /**
     * Асинхронно запускает статический конвейер по его имени.
     * <p>
     * Метод находит предопределенную последовательность агентов и выполняет их
     * один за другим, используя {@link CompletableFuture} для построения
     * асинхронной цепочки. Контекст (`AgentContext`) обогащается результатами
     * работы каждого агента и передается следующему.
     *
     * @param pipelineName   Уникальное имя конвейера, определенное в {@code definePipelines}.
     * @param initialContext Начальный контекст с входными данными для первого агента.
     * @return {@link CompletableFuture}, который по завершении всего конвейера
     * будет содержать полный список результатов от каждого выполненного агента.
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
     * экземпляр с обновленным контекстом и списком результатов.
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
