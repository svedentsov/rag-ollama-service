package com.example.ragollama.agent;

import com.example.ragollama.shared.exception.ProcessingException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Универсальный сервис-оркестратор, который управляет выполнением как
 * конвейеров ({@link AgentPipeline}), так и отдельных агентов ({@link QaAgent}).
 * <p>
 * Эталонная реализация, следующая принципам Clean Architecture и потокобезопасности.
 * Оркестратор реализует модель "Staged Execution" (поэтапное выполнение):
 * <ul>
 *     <li>Агенты внутри одного этапа выполняются <b>параллельно</b>.</li>
 *     <li>Этапы выполняются строго <b>последовательно</b>.</li>
 * </ul>
 * <p>
 * <b>Архитектурное решение:</b> Оркестратор использует паттерн
 * "Эволюционирующий Контекст" (Evolving Context). После выполнения каждого этапа,
 * результаты всех его агентов объединяются и добавляются в {@link AgentContext}
 * для следующего этапа. Это делает поток данных внутри конвейера явным и предсказуемым.
 */
@Slf4j
@Service
public class AgentOrchestratorService {

    private final Map<String, AgentPipeline> pipelines;
    private final Map<String, QaAgent> singleAgents;
    private final AsyncTaskExecutor applicationTaskExecutor;

    /**
     * Конструктор, который автоматически обнаруживает все доступные конвейеры и агенты,
     * инициализируя их реестры.
     *
     * @param pipelineProvider Провайдер для ленивого получения списка бинов конвейеров.
     * @param agentProvider Провайдер для ленивого получения списка всех агентов.
     * @param applicationTaskExecutor Пул потоков для асинхронного выполнения.
     */
    public AgentOrchestratorService(ObjectProvider<List<AgentPipeline>> pipelineProvider,
                                    ObjectProvider<List<QaAgent>> agentProvider,
                                    AsyncTaskExecutor applicationTaskExecutor) {
        this.applicationTaskExecutor = applicationTaskExecutor;
        List<AgentPipeline> pipelineBeans = pipelineProvider.getIfAvailable(Collections::emptyList);
        this.pipelines = pipelineBeans.stream()
                .collect(Collectors.toUnmodifiableMap(AgentPipeline::getName, Function.identity()));

        List<QaAgent> agentBeans = agentProvider.getIfAvailable(Collections::emptyList);
        this.singleAgents = agentBeans.stream()
                .collect(Collectors.toUnmodifiableMap(QaAgent::getName, Function.identity()));
    }

    /**
     * Инициализирует сервис и проверяет конфигурацию на наличие конфликтов имен.
     *
     * @throws IllegalStateException если обнаружена коллизия имен.
     */
    @PostConstruct
    public void init() {
        log.info("AgentOrchestratorService инициализирован. Зарегистрировано {} конвейеров: {}",
                pipelines.size(), pipelines.keySet());
        log.info("Зарегистрировано {} одиночных агентов: {}", singleAgents.size(), singleAgents.keySet());

        Set<String> commonNames = new HashSet<>(pipelines.keySet());
        commonNames.retainAll(singleAgents.keySet());

        if (!commonNames.isEmpty()) {
            String errorMsg = "Критическая ошибка конфигурации: Обнаружены дублирующиеся имена между конвейерами и агентами: " + commonNames;
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }

    /**
     * Предоставляет объединенный список имен всех доступных компонентов.
     *
     * @return Неизменяемое множество всех имен.
     */
    public Set<String> getAvailableComponentNames() {
        return Stream.concat(pipelines.keySet().stream(), singleAgents.keySet().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Асинхронно запускает конвейер или одиночного агента по его имени.
     *
     * @param name Уникальное имя конвейера или агента.
     * @param initialContext Начальный контекст с входными данными.
     * @return {@link CompletableFuture} с полным списком результатов от всех
     * выполненных агентов.
     * @throws ProcessingException если компонент с указанным именем не найден.
     */
    public CompletableFuture<List<AgentResult>> invoke(String name, AgentContext initialContext) {
        if (pipelines.containsKey(name)) {
            return invokePipeline(pipelines.get(name), initialContext);
        } else if (singleAgents.containsKey(name)) {
            return invokeSingleAgent(singleAgents.get(name), initialContext);
        }
        log.error("Попытка вызова несуществующего компонента: '{}'.", name);
        return CompletableFuture.failedFuture(
                new ProcessingException("Компонент (конвейер или агент) с именем '" + name + "' не найден.")
        );
    }

    private CompletableFuture<List<AgentResult>> invokeSingleAgent(QaAgent agent, AgentContext context) {
        log.info("Запуск одиночного агента '{}'.", agent.getName());
        return agent.execute(context).thenApply(List::of);
    }

    private CompletableFuture<List<AgentResult>> invokePipeline(AgentPipeline pipeline, AgentContext initialContext) {
        List<List<QaAgent>> stages = pipeline.getStages();
        log.info("Запуск конвейера '{}' с {} этапами.", pipeline.getName(), stages.size());

        var initialState = new StageExecutionState(initialContext, new ArrayList<>());
        CompletableFuture<StageExecutionState> executionChain = CompletableFuture.completedFuture(initialState);
        for (List<QaAgent> stageAgents : stages) {
            executionChain = executionChain.thenComposeAsync(
                    currentState -> executeStageAndUpdateState(stageAgents, currentState, pipeline.getName()),
                    applicationTaskExecutor
            );
        }
        return executionChain.thenApply(StageExecutionState::accumulatedResults);
    }

    private CompletableFuture<StageExecutionState> executeStageAndUpdateState(List<QaAgent> agentsInStage, StageExecutionState currentState, String pipelineName) {
        return executeStage(agentsInStage, currentState.currentContext(), pipelineName)
                .thenApply(stageResults -> {
                    Map<String, Object> newPayload = new HashMap<>(currentState.currentContext().payload());
                    stageResults.forEach(result -> newPayload.putAll(result.details()));
                    AgentContext newContext = new AgentContext(newPayload);
                    List<AgentResult> newAccumulatedResults = new ArrayList<>(currentState.accumulatedResults());
                    newAccumulatedResults.addAll(stageResults);
                    return new StageExecutionState(newContext, newAccumulatedResults);
                });
    }

    private CompletableFuture<List<AgentResult>> executeStage(List<QaAgent> agentsInStage, AgentContext context, String pipelineName) {
        log.debug("Выполнение этапа в конвейере '{}' с {} агентами.", pipelineName, agentsInStage.size());
        List<CompletableFuture<AgentResult>> stageFutures = agentsInStage.stream()
                .filter(agent -> agent.canHandle(context))
                .map(agent -> agent.execute(context))
                .toList();
        return CompletableFuture.allOf(stageFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> stageFutures.stream()
                        .map(CompletableFuture::join)
                        .toList()
                );
    }

    private record StageExecutionState(AgentContext currentContext, List<AgentResult> accumulatedResults) {
    }
}
