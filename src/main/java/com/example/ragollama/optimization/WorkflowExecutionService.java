package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.registry.ToolRegistryService;
import com.example.ragollama.optimization.model.WorkflowNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис-исполнитель для динамически сгенерированных рабочих процессов (workflows),
 * представленных в виде направленного ациклического графа (DAG).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowExecutionService {

    private final ToolRegistryService toolRegistryService;

    /**
     * Выполняет рабочий процесс, описанный в виде графа.
     *
     * @param workflow       Список узлов, описывающих граф.
     * @param initialContext Начальный контекст.
     * @return {@link Mono} с финальным списком результатов всех выполненных узлов.
     */
    public Mono<List<AgentResult>> executeWorkflow(List<WorkflowNode> workflow, AgentContext initialContext) {
        if (workflow == null || workflow.isEmpty()) {
            return Mono.just(List.of());
        }
        Map<String, WorkflowNode> nodeMap = workflow.stream()
                .collect(Collectors.toMap(WorkflowNode::id, Function.identity()));
        // Мемоизация (кэширование) результатов выполнения каждого узла
        Map<String, Mono<AgentResult>> memo = new HashMap<>();
        // Запускаем выполнение для каждого узла графа
        List<Mono<AgentResult>> allNodeMonos = workflow.stream()
                .map(node -> executeNode(node, nodeMap, memo, initialContext))
                .toList();
        // Собираем результаты всех узлов после их завершения
        return Flux.merge(allNodeMonos).collectList();
    }

    private Mono<AgentResult> executeNode(
            WorkflowNode node,
            Map<String, WorkflowNode> nodeMap,
            Map<String, Mono<AgentResult>> memo,
            AgentContext initialContext) {
        // Проверяем кэш. Если Mono для этого узла уже создан, возвращаем его.
        if (memo.containsKey(node.id())) {
            return memo.get(node.id());
        }
        // Собираем Mono's для всех зависимостей этого узла
        List<Mono<AgentResult>> dependencyMonos = node.dependencies().stream()
                .map(depId -> nodeMap.get(depId))
                .map(depNode -> executeNode(depNode, nodeMap, memo, initialContext))
                .toList();

        // Создаем Mono, который будет выполнен только после завершения всех зависимостей
        Mono<AgentResult> currentNodeMono = Mono.defer(() -> {
            // Mono.when() ожидает завершения всех зависимостей
            return Mono.when(dependencyMonos)
                    .then(Mono.fromRunnable(() -> log.info("Все зависимости для узла '{}' выполнены. Запуск...", node.id())))
                    .then(Mono.zip(Mono.just(dependencyMonos), Mono.just(initialContext)))
                    .flatMap(data -> {
                        // Собираем контекст из начального и результатов зависимостей
                        Map<String, Object> newContextPayload = new HashMap<>(data.getT2().payload());
                        data.getT1().forEach(depMono ->
                                depMono.doOnNext(depResult -> newContextPayload.putAll(depResult.details())).block()
                        );
                        newContextPayload.putAll(node.arguments());
                        AgentContext finalContext = new AgentContext(newContextPayload);

                        QaAgent agent = toolRegistryService.getAgent(node.agentName())
                                .orElseThrow(() -> new IllegalStateException("Агент не найден: " + node.agentName()));

                        log.info("Выполнение узла '{}' (агент: {})", node.id(), node.agentName());
                        return agent.execute(finalContext)
                                .subscribeOn(Schedulers.boundedElastic()); // Выполняем на отдельном потоке
                    });
        }).cache(); // Кэшируем результат Mono, чтобы он не выполнялся повторно
        // Сохраняем Mono в кэш
        memo.put(node.id(), currentNodeMono);
        return currentNodeMono;
    }
}
