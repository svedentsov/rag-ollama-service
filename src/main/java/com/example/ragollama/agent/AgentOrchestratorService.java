package com.example.ragollama.agent;

import com.example.ragollama.shared.exception.ProcessingException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Универсальный сервис-оркестратор, полностью переведенный на Project Reactor.
 *
 * <p>Эталонная реализация, следующая принципам Clean Architecture и потокобезопасности.
 * Оркестратор реализует модель "Staged Execution" (поэтапное выполнение) в реактивном стиле:
 * <ul>
 *     <li>Агенты внутри одного этапа выполняются <b>параллельно</b>.</li>
 *     <li>Этапы выполняются строго <b>последовательно</b>.</li>
 * </ul>
 * <p><b>Архитектурное решение:</b> Оркестратор использует паттерн
 * "Эволюционирующий Контекст" (Evolving Context). Результаты каждого этапа
 * объединяются и добавляются в {@link AgentContext} для следующего.
 */
@Slf4j
@Service
public class AgentOrchestratorService {

    private final Map<String, AgentPipeline> pipelines;
    private final Map<String, QaAgent> singleAgents;

    /**
     * Конструктор, который автоматически обнаруживает все доступные конвейеры и агенты
     * в контексте Spring, используя {@link ObjectProvider} для ленивой инициализации.
     *
     * <p>Это делает систему легко расширяемой: для добавления нового конвейера или
     * агента достаточно создать соответствующий бин.
     *
     * @param pipelineProvider Провайдер для ленивого получения списка бинов конвейеров.
     * @param agentProvider    Провайдер для ленивого получения списка всех агентов.
     */
    public AgentOrchestratorService(ObjectProvider<List<AgentPipeline>> pipelineProvider,
                                    ObjectProvider<List<QaAgent>> agentProvider) {
        List<AgentPipeline> pipelineBeans = pipelineProvider.getIfAvailable(Collections::emptyList);
        this.pipelines = pipelineBeans.stream()
                .collect(Collectors.toUnmodifiableMap(AgentPipeline::getName, Function.identity()));

        List<QaAgent> agentBeans = agentProvider.getIfAvailable(Collections::emptyList);
        this.singleAgents = agentBeans.stream()
                .collect(Collectors.toUnmodifiableMap(QaAgent::getName, Function.identity()));
    }

    /**
     * Инициализирует сервис после внедрения зависимостей и выполняет самопроверку
     * конфигурации на предмет конфликтов имен между конвейерами и одиночными агентами.
     *
     * @throws IllegalStateException если обнаружена коллизия имен, что может привести
     *                               к неоднозначности при вызове.
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
     * Предоставляет объединенный, неизменяемый список имен всех доступных для вызова
     * компонентов (конвейеров и одиночных агентов).
     *
     * @return Неизменяемое множество всех зарегистрированных имен.
     */
    public Set<String> getAvailableComponentNames() {
        return Stream.concat(pipelines.keySet().stream(), singleAgents.keySet().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Асинхронно запускает конвейер или одиночного агента по его имени.
     *
     * <p>Этот метод является единой точкой входа для всей бизнес-логики. Он
     * определяет, является ли запрошенный компонент конвейером или одиночным
     * агентом, и делегирует выполнение соответствующему внутреннему методу.
     *
     * @param name           Уникальное имя конвейера или агента.
     * @param initialContext Начальный контекст с входными данными.
     * @return {@link Mono}, который по завершении будет содержать полный список
     * результатов от всех выполненных в рамках вызова агентов.
     * @throws ProcessingException если компонент с указанным именем не найден.
     */
    public Mono<List<AgentResult>> invoke(String name, AgentContext initialContext) {
        if (pipelines.containsKey(name)) {
            return invokePipeline(pipelines.get(name), initialContext);
        } else if (singleAgents.containsKey(name)) {
            return invokeSingleAgent(singleAgents.get(name), initialContext);
        }
        log.error("Попытка вызова несуществующего компонента: '{}'.", name);
        return Mono.error(new ProcessingException("Компонент (конвейер или агент) с именем '" + name + "' не найден."));
    }

    /**
     * Запускает одиночного агента и обертывает его результат в список.
     *
     * @param agent   Агент для запуска.
     * @param context Контекст для выполнения.
     * @return {@link Mono} со списком, содержащим один результат работы агента.
     */
    private Mono<List<AgentResult>> invokeSingleAgent(QaAgent agent, AgentContext context) {
        log.info("Запуск одиночного агента '{}'.", agent.getName());
        return agent.execute(context).map(List::of);
    }

    /**
     * Запускает полный конвейер в полностью неблокирующем, реактивном стиле.
     * Последовательно выполняет каждый этап, передавая обогащенный контекст
     * от предыдущего этапа к следующему.
     *
     * @param pipeline       Конвейер для запуска.
     * @param initialContext Начальный контекст.
     * @return {@link Mono} с агрегированным списком результатов всех агентов из всех этапов.
     */
    private Mono<List<AgentResult>> invokePipeline(AgentPipeline pipeline, AgentContext initialContext) {
        List<List<QaAgent>> stages = pipeline.getStages();
        log.info("Запуск конвейера '{}' с {} этапами.", pipeline.getName(), stages.size());

        var initialState = new StageExecutionState(initialContext, new ArrayList<>());

        // Последовательно выполняем каждый этап, передавая обновленное состояние.
        // `reduce` с Mono-аккумулятором и `flatMap` является идиоматичным способом
        // для последовательного выполнения асинхронных операций в Project Reactor.
        return Flux.fromIterable(stages)
                .reduce(Mono.just(initialState), (stateMono, stageAgents) ->
                        stateMono.flatMap(currentState -> executeStageAndUpdateState(stageAgents, currentState, pipeline.getName()))
                )
                .flatMap(finalStateMono -> finalStateMono.map(StageExecutionState::accumulatedResults));
    }

    /**
     * Выполняет один этап конвейера (всех его агентов параллельно) и асинхронно
     * обновляет общее состояние конвейера для передачи на следующий этап.
     *
     * @param agentsInStage Агенты для параллельного выполнения в данном этапе.
     * @param currentState  Текущее состояние (контекст и результаты).
     * @param pipelineName  Имя конвейера для логирования.
     * @return {@link Mono} с новым, обновленным состоянием конвейера.
     */
    private Mono<StageExecutionState> executeStageAndUpdateState(List<QaAgent> agentsInStage, StageExecutionState currentState, String pipelineName) {
        return executeStage(agentsInStage, currentState.currentContext(), pipelineName)
                .map(stageResults -> {
                    // Создаем новый, обогащенный контекст для следующего этапа,
                    // используя результаты текущего (Паттерн "Эволюционирующий Контекст").
                    Map<String, Object> newPayload = new HashMap<>(currentState.currentContext().payload());
                    stageResults.forEach(result -> newPayload.putAll(result.details()));
                    AgentContext newContext = new AgentContext(newPayload);

                    // Добавляем результаты текущего этапа в общий список
                    List<AgentResult> newAccumulatedResults = new ArrayList<>(currentState.accumulatedResults());
                    newAccumulatedResults.addAll(stageResults);

                    return new StageExecutionState(newContext, newAccumulatedResults);
                });
    }

    /**
     * Выполняет всех агентов одного этапа параллельно.
     *
     * @param agentsInStage Список агентов для выполнения.
     * @param context       Контекст для выполнения.
     * @param pipelineName  Имя конвейера для логирования.
     * @return {@link Mono} со списком результатов всех агентов этого этапа.
     */
    private Mono<List<AgentResult>> executeStage(List<QaAgent> agentsInStage, AgentContext context, String pipelineName) {
        log.debug("Выполнение этапа в конвейере '{}' с {} агентами.", pipelineName, agentsInStage.size());
        return Flux.fromIterable(agentsInStage)
                .filter(agent -> {
                    boolean canHandle = agent.canHandle(context);
                    if (!canHandle) {
                        log.trace("Агент '{}' пропущен, так как не может обработать текущий контекст.", agent.getName());
                    }
                    return canHandle;
                })
                .flatMap(agent -> agent.execute(context)
                        // Каждый агент может выполнять блокирующие операции, поэтому его
                        // выполнение выносится на отдельный эластичный планировщик,
                        // чтобы не блокировать основной event-loop.
                        .subscribeOn(Schedulers.boundedElastic()))
                .collectList();
    }

    /**
     * Внутренний record для безопасной и неизменяемой передачи промежуточного
     * состояния выполнения конвейера между этапами.
     *
     * @param currentContext     Текущий, обогащенный контекст.
     * @param accumulatedResults Список всех результатов от предыдущих этапов.
     */
    private record StageExecutionState(AgentContext currentContext, List<AgentResult> accumulatedResults) {
    }
}
