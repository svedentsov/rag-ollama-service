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
 * <p>
 * Он автоматически обнаруживает все доступные реализации {@link QaAgent}
 * и позволяет запускать их в виде предопределенных последовательных цепочек (пайплайнов).
 * Это обеспечивает высокую расширяемость и следование принципу Open/Closed.
 */
@Slf4j
@Service
public class AgentOrchestratorService {

    private final Map<String, QaAgent> agentMap;
    private final Map<String, List<QaAgent>> pipelines;

    /**
     * Конструктор, который автоматически собирает все бины типа {@link QaAgent}
     * и инициализирует предопределенные конвейеры.
     *
     * @param agents Список всех реализаций {@link QaAgent}, найденных в контексте Spring.
     */
    public AgentOrchestratorService(List<QaAgent> agents) {
        this.agentMap = agents.stream()
                .collect(Collectors.toMap(QaAgent::getName, Function.identity()));
        this.pipelines = definePipelines();
        log.info("AgentOrchestratorService инициализирован. Доступные агенты: {}. Доступные конвейеры: {}",
                agentMap.keySet(), pipelines.keySet());
    }

    /**
     * Определяет статические конвейеры (цепочки) выполнения агентов.
     * В production-системе эта конфигурация может быть вынесена в YAML-файл.
     *
     * @return Карта, где ключ - имя конвейера, а значение - упорядоченный список агентов.
     */
    private Map<String, List<QaAgent>> definePipelines() {
        return Map.of(
                "github-pr-pipeline", List.of(
                        agentMap.get("test-prioritizer")
                ),
                "jira-bug-pipeline", List.of(
                        agentMap.get("bug-duplicate-detector")
                )
        );
    }

    /**
     * Асинхронно выполняет именованный конвейер агентов.
     * <p>
     * Метод последовательно выполняет каждого агента из конвейера, передавая
     * и обогащая {@link AgentContext} на каждом шаге. Результаты всех агентов
     * агрегируются в итоговый список.
     *
     * @param pipelineName   Имя конвейера для запуска.
     * @param initialContext Начальный контекст с входными данными.
     * @return {@link CompletableFuture} с агрегированными результатами всех агентов.
     */
    public CompletableFuture<List<AgentResult>> invokePipeline(String pipelineName, AgentContext initialContext) {
        List<QaAgent> agentsInPipeline = pipelines.get(pipelineName);
        if (agentsInPipeline == null || agentsInPipeline.isEmpty()) {
            log.warn("Конвейер с именем '{}' не найден или пуст.", pipelineName);
            return CompletableFuture.completedFuture(List.of());
        }

        log.info("Запуск конвейера '{}' с {} агентами.", pipelineName, agentsInPipeline.size());

        // Создаем цепочку асинхронных вызовов
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

    /**
     * Внутренний неизменяемый класс для хранения состояния выполнения конвейера.
     * <p>
     * Вместо изменения одного объекта, каждый шаг создает новый экземпляр,
     * что делает логику более предсказуемой и безопасной в асинхронной среде.
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
         * @return Новый экземпляр {@link PipelineExecutionState}.
         */
        public PipelineExecutionState addResult(AgentResult newResult) {
            List<AgentResult> newResults = new java.util.ArrayList<>(this.results);
            newResults.add(newResult);
            this.currentContext.payload().putAll(newResult.details());
            return new PipelineExecutionState(this.currentContext, newResults);
        }

        public List<AgentResult> getResults() {
            return results;
        }
    }
}
