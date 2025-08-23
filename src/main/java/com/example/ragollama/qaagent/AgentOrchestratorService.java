package com.example.ragollama.qaagent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис-оркестратор, управляющий выполнением конвейеров из QA-агентов.
 */
@Slf4j
@Service
public class AgentOrchestratorService {

    private final Map<String, QaAgent> agentMap;
    private final Map<String, List<QaAgent>> pipelines;

    public AgentOrchestratorService(List<QaAgent> agents) {
        this.agentMap = agents.stream()
                .collect(Collectors.toMap(QaAgent::getName, Function.identity()));
        this.pipelines = definePipelines();
        log.info("AgentOrchestratorService инициализирован. Доступные агенты: {}. Доступные конвейеры: {}",
                agentMap.keySet(), pipelines.keySet());
    }

    private Map<String, List<QaAgent>> definePipelines() {
        return Map.ofEntries(
                Map.entry("git-inspector-pipeline", List.of(agentMap.get("git-inspector"))),
                Map.entry("openapi-pipeline", List.of(agentMap.get("openapi-agent"))),
                Map.entry("flaky-test-detection-pipeline", List.of(agentMap.get("flaky-test-detector"))),
                Map.entry("spec-drift-sentinel-pipeline", List.of(agentMap.get("spec-drift-sentinel"))),
                Map.entry("github-pr-pipeline", List.of(agentMap.get("test-prioritizer"))),
                Map.entry("jira-bug-creation-pipeline", List.of(agentMap.get("bug-duplicate-detector"))),
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
                        agentMap.get("rbac-extractor"))
                ),
                Map.entry("deep-security-audit-pipeline", List.of(
                        agentMap.get("git-inspector"),
                        agentMap.get("rbac-extractor"),
                        agentMap.get("auth-risk-detector"))
                ),
                Map.entry("root-cause-analysis-pipeline", List.of(
                        agentMap.get("flaky-test-detector"),
                        agentMap.get("git-inspector"),
                        agentMap.get("root-cause-analyzer"))
                ),
                // НОВЫЙ КОМПОЗИТНЫЙ КОНВЕЙЕР
                Map.entry("impact-analysis-pipeline", List.of(
                        agentMap.get("git-inspector"),
                        agentMap.get("impact-analyzer"))
                )
        );
    }

    public CompletableFuture<List<AgentResult>> invokePipeline(String pipelineName, AgentContext initialContext) {
        List<QaAgent> agentsInPipeline = pipelines.get(pipelineName);
        if (agentsInPipeline == null || agentsInPipeline.isEmpty()) {
            log.warn("Конвейер с именем '{}' не найден или пуст.", pipelineName);
            return CompletableFuture.completedFuture(List.of());
        }

        log.info("Запуск конвейера '{}' с {} агентами.", pipelineName, agentsInPipeline.size());

        CompletableFuture<PipelineExecutionState> executionChain = CompletableFuture.completedFuture(
                new PipelineExecutionState(initialContext, List.of())
        );

        for (QaAgent agent : agentsInPipeline) {
            executionChain = executionChain.thenCompose(state -> {
                log.debug("Выполнение агента '{}' в конвейере '{}'", agent.getName(), pipelineName);
                return agent.execute(state.currentContext)
                        .thenApply(state::addResult);
            });
        }

        return executionChain.thenApply(PipelineExecutionState::getResults);
    }

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
         * @return Новый экземпляр {@link PipelineExecutionState}.
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
