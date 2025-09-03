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
 *
 * <p>Эта версия реализует модель "Staged Execution" (поэтапное выполнение).
 * Оркестратор выполняет агентов внутри одного этапа параллельно, а сами
 * этапы — последовательно. Это позволяет значительно повысить производительность
 * за счет распараллеливания независимых I/O-bound задач.
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
     * <p>
     * Этот конструктор является единственным в классе, что соответствует
     * лучшим практикам Dependency Injection. Он явно принимает все
     * необходимые зависимости.
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
     * Предоставляет публичный доступ к списку имен всех
     * зарегистрированных конвейеров.
     *
     * @return Неизменяемое множество имен конвейеров.
     */
    public Set<String> getAvailablePipelines() {
        return pipelines.keySet();
    }

    /**
     * Асинхронно запускает конвейер по его имени, используя модель поэтапного выполнения.
     *
     * <p>Метод находит соответствующую стратегию {@link AgentPipeline}, получает от нее
     * список этапов и последовательно выполняет каждый из них. Агенты внутри одного
     * этапа запускаются параллельно.
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

        PipelineExecutionState initialState = new PipelineExecutionState(initialContext, List.of());
        CompletableFuture<PipelineExecutionState> executionChain = CompletableFuture.completedFuture(initialState);

        for (List<QaAgent> stageAgents : stages) {
            executionChain = executionChain.thenComposeAsync(
                    currentState -> executeStage(stageAgents, currentState, pipelineName),
                    applicationTaskExecutor
            );
        }

        return executionChain.thenApply(PipelineExecutionState::results);
    }

    /**
     * Параллельно выполняет всех агентов одного этапа.
     *
     * @param agentsInStage Список агентов для параллельного запуска.
     * @param state         Текущее состояние конвейера.
     * @param pipelineName  Имя конвейера (для логирования).
     * @return {@link CompletableFuture} с новым состоянием после завершения этапа.
     */
    private CompletableFuture<PipelineExecutionState> executeStage(List<QaAgent> agentsInStage, PipelineExecutionState state, String pipelineName) {
        log.debug("Выполнение этапа в конвейере '{}' с {} агентами.", pipelineName, agentsInStage.size());

        List<CompletableFuture<AgentResult>> stageFutures = agentsInStage.stream()
                .filter(agent -> agent.canHandle(state.currentContext))
                .map(agent -> agent.execute(state.currentContext))
                .toList();

        return CompletableFuture.allOf(stageFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<AgentResult> stageResults = stageFutures.stream()
                            .map(CompletableFuture::join)
                            .toList();
                    return state.addResults(stageResults);
                });
    }

    /**
     * Внутренний неизменяемый (immutable) record для хранения текущего состояния
     * выполнения конвейера. Использование record сокращает бойлерплейт и
     * гарантирует потокобезопасность.
     *
     * @param currentContext Текущий контекст, передаваемый между агентами.
     * @param results        Список результатов от уже выполненных агентов.
     */
    private record PipelineExecutionState(AgentContext currentContext, List<AgentResult> results) {
        /**
         * Создает новый экземпляр состояния, добавляя результаты последнего
         * выполненного этапа и обогащая контекст для следующего.
         *
         * @param newResults Результаты работы агентов предыдущего этапа.
         * @return Новый, обновленный экземпляр {@link PipelineExecutionState}.
         */
        public PipelineExecutionState addResults(List<AgentResult> newResults) {
            if (newResults == null || newResults.isEmpty()) {
                return this;
            }
            List<AgentResult> updatedResults = new ArrayList<>(this.results);
            updatedResults.addAll(newResults);
            Map<String, Object> newPayload = new HashMap<>(this.currentContext.payload());
            newResults.forEach(result -> newPayload.putAll(result.details()));
            return new PipelineExecutionState(new AgentContext(newPayload), Collections.unmodifiableList(updatedResults));
        }
    }
}
