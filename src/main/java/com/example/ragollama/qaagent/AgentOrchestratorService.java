package com.example.ragollama.qaagent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис-оркестратор, управляющий выполнением пайплайнов из QA-агентов.
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
    }

    private Map<String, List<QaAgent>> definePipelines() {
        // Определяем наши пайплайны здесь. В production это можно вынести в конфигурацию.
        return Map.of(
                "github-pr-pipeline", List.of(
                        agentMap.get("test-prioritizer"),
                        agentMap.get("pr-test-coverage-gate")
                ),
                "jira-bug-pipeline", List.of(
                        agentMap.get("bug-duplicate-detector")
                )
        );
    }

    /**
     * Асинхронно выполняет именованный пайплайн агентов.
     *
     * @param pipelineName   Имя пайплайна для запуска.
     * @param initialContext Начальный контекст.
     * @return {@link CompletableFuture} с агрегированными результатами всех агентов.
     */
    public CompletableFuture<List<AgentResult>> invokePipeline(String pipelineName, AgentContext initialContext) {
        List<QaAgent> agentsInPipeline = pipelines.get(pipelineName);
        if (agentsInPipeline == null || agentsInPipeline.isEmpty()) {
            log.warn("Пайплайн с именем '{}' не найден или пуст.", pipelineName);
            return CompletableFuture.completedFuture(List.of());
        }

        log.info("Запуск пайплайна '{}' с {} агентами.", pipelineName, agentsInPipeline.size());

        CompletableFuture<PipelineExecutionState> executionChain = CompletableFuture.completedFuture(
                new PipelineExecutionState(initialContext, List.of())
        );

        for (QaAgent agent : agentsInPipeline) {
            executionChain = executionChain.thenCompose(state ->
                    agent.execute(state.currentContext)
                            .thenApply(result -> state.addResult(result))
            );
        }

        return executionChain.thenApply(PipelineExecutionState::getResults);
    }

    /**
     * Внутренний класс для хранения состояния выполнения пайплайна.
     */
    private static class PipelineExecutionState {
        private final AgentContext currentContext;
        private final List<AgentResult> results;

        PipelineExecutionState(AgentContext currentContext, List<AgentResult> results) {
            this.currentContext = currentContext;
            this.results = new java.util.ArrayList<>(results);
        }

        public PipelineExecutionState addResult(AgentResult newResult) {
            this.results.add(newResult);
            // Мержим детали результата в контекст для следующих агентов
            this.currentContext.payload().putAll(newResult.details());
            return new PipelineExecutionState(this.currentContext, this.results);
        }

        public List<AgentResult> getResults() {
            return results;
        }
    }
}
