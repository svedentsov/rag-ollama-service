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

/**
 * Сервис-оркестратор, который управляет выполнением конвейеров AI-агентов.
 * <p>
 * Эталонная реализация, следующая принципам Clean Architecture и потокобезопасности.
 * Оркестратор реализует модель "Staged Execution" (поэтапное выполнение):
 * <ul>
 *     <li>Агенты внутри одного этапа выполняются <b>параллельно</b>.</li>
 *     <li>Этапы выполняются строго <b>последовательно</b>.</li>
 * </ul>
 * Это позволяет значительно повысить производительность за счет распараллеливания
 * независимых I/O-bound задач (например, одновременный запрос к Git и базе данных).
 * <p>
 * <b>Архитектурное решение:</b> Оркестратор использует паттерн
 * "Эволюционирующий Контекст" (Evolving Context). После выполнения каждого этапа,
 * результаты всех его агентов объединяются и добавляются в {@link AgentContext}
 * для следующего этапа. Это делает поток данных внутри конвейера явным,
 * предсказуемым и легко тестируемым.
 *
 * @see AgentPipeline
 */
@Slf4j
@Service
public class AgentOrchestratorService {

    private final Map<String, AgentPipeline> pipelines;
    private final AsyncTaskExecutor applicationTaskExecutor;

    /**
     * Конструктор, который автоматически обнаруживает все доступные конвейеры
     * (бины, реализующие {@link AgentPipeline}) и инициализирует их реестр.
     *
     * @param pipelineProvider        Провайдер для ленивого получения списка бинов конвейеров.
     * @param applicationTaskExecutor Пул потоков для асинхронного выполнения.
     */
    public AgentOrchestratorService(ObjectProvider<List<AgentPipeline>> pipelineProvider, AsyncTaskExecutor applicationTaskExecutor) {
        this.applicationTaskExecutor = applicationTaskExecutor;
        List<AgentPipeline> pipelineBeans = pipelineProvider.getIfAvailable(Collections::emptyList);
        this.pipelines = pipelineBeans.stream()
                .collect(Collectors.toUnmodifiableMap(AgentPipeline::getName, Function.identity()));
    }

    /**
     * Инициализирует сервис и логирует информацию о зарегистрированных конвейерах.
     * Этот метод вызывается автоматически после создания бина.
     */
    @PostConstruct
    public void init() {
        log.info("AgentOrchestratorService инициализирован. Зарегистрировано {} конвейеров: {}",
                pipelines.size(), pipelines.keySet());
    }

    /**
     * Предоставляет публичный доступ к списку имен всех зарегистрированных конвейеров.
     *
     * @return Неизменяемое множество имен конвейеров.
     */
    public Set<String> getAvailablePipelines() {
        return pipelines.keySet();
    }

    /**
     * Асинхронно запускает конвейер по его имени, используя модель поэтапного выполнения
     * и эволюционирующий контекст.
     *
     * <p>Метод находит соответствующую стратегию {@link AgentPipeline}, получает от нее
     * список этапов и последовательно выполняет каждый из них. Агенты внутри одного
     * этапа запускаются параллельно. Результаты каждого этапа обогащают контекст
     * для следующего.
     *
     * @param pipelineName   Уникальное имя конвейера.
     * @param initialContext Начальный контекст с входными данными.
     * @return {@link CompletableFuture} с полным списком результатов от всех
     * агентов, выполненных в конвейере.
     * @throws ProcessingException если конвейер с указанным именем не найден.
     */
    public CompletableFuture<List<AgentResult>> invokePipeline(String pipelineName, AgentContext initialContext) {
        AgentPipeline pipeline = pipelines.get(pipelineName);
        if (pipeline == null) {
            log.error("Попытка вызова несуществующего конвейера: '{}'. Доступные конвейеры: {}",
                    pipelineName, pipelines.keySet());
            return CompletableFuture.failedFuture(
                    new ProcessingException("Конвейер с именем '" + pipelineName + "' не найден.")
            );
        }

        List<List<QaAgent>> stages = pipeline.getStages();
        log.info("Запуск конвейера '{}' с {} этапами.", pipelineName, stages.size());

        // Начальное состояние для цепочки: кортеж из начального контекста и пустого списка результатов
        var initialState = new StageExecutionState(initialContext, new ArrayList<>());

        // Последовательно выполняем этапы, передавая и обогащая состояние (контекст и результаты)
        CompletableFuture<StageExecutionState> executionChain = CompletableFuture.completedFuture(initialState);
        for (List<QaAgent> stageAgents : stages) {
            executionChain = executionChain.thenComposeAsync(
                    currentState -> executeStageAndUpdateState(stageAgents, currentState, pipelineName),
                    applicationTaskExecutor
            );
        }

        // В конце извлекаем только список всех результатов
        return executionChain.thenApply(StageExecutionState::accumulatedResults);
    }

    /**
     * Выполняет один этап и обновляет общее состояние конвейера.
     *
     * @param agentsInStage Список агентов для параллельного запуска.
     * @param currentState  Текущее состояние выполнения (контекст и результаты).
     * @param pipelineName  Имя конвейера (для логирования).
     * @return {@link CompletableFuture} с новым, обновленным состоянием.
     */
    private CompletableFuture<StageExecutionState> executeStageAndUpdateState(List<QaAgent> agentsInStage, StageExecutionState currentState, String pipelineName) {
        return executeStage(agentsInStage, currentState.currentContext(), pipelineName)
                .thenApply(stageResults -> {
                    // Создаем новый, обогащенный контекст для следующего этапа
                    Map<String, Object> newPayload = new HashMap<>(currentState.currentContext().payload());
                    stageResults.forEach(result -> newPayload.putAll(result.details()));
                    AgentContext newContext = new AgentContext(newPayload);

                    // Добавляем результаты этого этапа в общий список
                    List<AgentResult> newAccumulatedResults = new ArrayList<>(currentState.accumulatedResults());
                    newAccumulatedResults.addAll(stageResults);

                    // Возвращаем новое состояние
                    return new StageExecutionState(newContext, newAccumulatedResults);
                });
    }


    /**
     * Параллельно выполняет всех агентов одного этапа.
     *
     * @param agentsInStage Список агентов для параллельного запуска.
     * @param context       Текущий контекст для всех агентов этапа.
     * @param pipelineName  Имя конвейера (для логирования).
     * @return {@link CompletableFuture} со списком результатов работы агентов этого этапа.
     */
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

    /**
     * Внутренний record для инкапсуляции состояния выполнения между этапами.
     */
    private record StageExecutionState(AgentContext currentContext, List<AgentResult> accumulatedResults) {
    }
}
