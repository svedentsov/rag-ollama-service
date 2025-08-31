package com.example.ragollama.agent;

import com.example.ragollama.shared.exception.ProcessingException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис-оркестратор, который управляет выполнением конвейеров AI-агентов.
 * <p>
 * Эта версия реализует паттерн "Стратегия". Оркестратор автоматически обнаруживает
 * все бины, реализующие {@link AgentPipeline}, и предоставляет API для их
 * асинхронного запуска по имени. Он не содержит логики композиции конвейеров,
 * а лишь делегирует выполнение соответствующей "стратегии" (реализации конвейера).
 * <p>
 * Внутренняя реализация использует функциональный подход с `Stream.reduce`
 * для построения асинхронной цепочки вызовов, что повышает читаемость и
 * соответствует современным практикам.
 *
 * @see AgentPipeline
 */
@Slf4j
@Service
public class AgentOrchestratorService {

    private final Map<String, AgentPipeline> pipelines;

    /**
     * Конструктор, который автоматически обнаруживает все доступные конвейеры
     * (бины, реализующие {@link AgentPipeline}) и инициализирует их реестр.
     *
     * @param pipelineProvider Провайдер для ленивого получения списка бинов конвейеров.
     */
    public AgentOrchestratorService(ObjectProvider<List<AgentPipeline>> pipelineProvider) {
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
     * Асинхронно запускает конвейер по его имени.
     * <p>
     * Метод находит соответствующую стратегию {@link AgentPipeline}, получает от нее
     * упорядоченный список агентов и последовательно выполняет каждого из них,
     * передавая и обогащая {@link AgentContext} на каждом шаге.
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

        List<QaAgent> agentsInPipeline = pipeline.getAgents();
        log.info("Запуск конвейера '{}' с {} агентами.", pipelineName, agentsInPipeline.size());
        PipelineExecutionState initialState = new PipelineExecutionState(initialContext, List.of());
        CompletableFuture<PipelineExecutionState> executionChain = agentsInPipeline.stream()
                .reduce(CompletableFuture.completedFuture(initialState),
                        (stateFuture, agent) -> stateFuture.thenCompose(state -> executeAgent(agent, state, pipelineName)),
                        (f1, f2) -> f1.thenCombine(f2, (s1, s2) -> s2)
                );
        return executionChain.thenApply(PipelineExecutionState::results);
    }

    /**
     * Выполняет одного агента и обновляет состояние конвейера.
     *
     * @param agent        Агент для выполнения.
     * @param state        Текущее состояние конвейера.
     * @param pipelineName Имя конвейера (для логирования).
     * @return {@link CompletableFuture} с новым состоянием.
     */
    private CompletableFuture<PipelineExecutionState> executeAgent(QaAgent agent, PipelineExecutionState state, String pipelineName) {
        log.debug("Выполнение агента '{}' в конвейере '{}'", agent.getName(), pipelineName);
        if (!agent.canHandle(state.currentContext)) {
            log.warn("Агент '{}' пропущен, так как не может обработать текущий контекст.", agent.getName());
            return CompletableFuture.completedFuture(state);
        }
        return agent.execute(state.currentContext)
                .thenApply(state::addResult);
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
         * Создает новый экземпляр состояния, добавляя результат последнего
         * выполненного агента и обогащая контекст для следующего шага.
         *
         * @param newResult Результат работы предыдущего агента.
         * @return Новый, обновленный экземпляр {@link PipelineExecutionState}.
         */
        public PipelineExecutionState addResult(AgentResult newResult) {
            List<AgentResult> newResults = new ArrayList<>(this.results);
            newResults.add(newResult);
            Map<String, Object> newPayload = new java.util.HashMap<>(this.currentContext.payload());
            newPayload.putAll(newResult.details());
            return new PipelineExecutionState(new AgentContext(newPayload), Collections.unmodifiableList(newResults));
        }
    }
}
