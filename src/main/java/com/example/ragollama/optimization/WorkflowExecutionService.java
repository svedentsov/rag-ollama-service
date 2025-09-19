package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.registry.ToolRegistryService;
import com.example.ragollama.optimization.model.WorkflowNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
        Map<String, CompletableFuture<AgentResult>> memo = new HashMap<>();

        List<CompletableFuture<AgentResult>> allFutures = workflow.stream()
                .map(node -> executeNode(node, nodeMap, memo, initialContext))
                .toList();

        return Mono.fromFuture(CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> allFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())));
    }

    /**
     * Рекурсивно выполняет один узел графа, предварительно дождавшись выполнения его зависимостей.
     * Использует мемоизацию для предотвращения повторного выполнения.
     *
     * @param node           Узел для выполнения.
     * @param nodeMap        Карта всех узлов для быстрого доступа.
     * @param memo           Кэш для хранения уже запущенных CompletableFuture.
     * @param initialContext Начальный контекст.
     * @return {@link CompletableFuture} с результатом выполнения этого узла.
     */
    private CompletableFuture<AgentResult> executeNode(
            WorkflowNode node,
            Map<String, WorkflowNode> nodeMap,
            Map<String, CompletableFuture<AgentResult>> memo,
            AgentContext initialContext) {

        if (memo.containsKey(node.id())) {
            return memo.get(node.id());
        }

        // 1. Рекурсивно получаем CompletableFuture для всех зависимостей
        List<CompletableFuture<AgentResult>> dependencyFutures = node.dependencies().stream()
                .map(depId -> nodeMap.get(depId))
                .map(depNode -> executeNode(depNode, nodeMap, memo, initialContext))
                .toList();

        // 2. Создаем CompletableFuture, который будет ждать завершения всех зависимостей
        CompletableFuture<Void> allDependencies = CompletableFuture.allOf(dependencyFutures.toArray(new CompletableFuture[0]));

        // 3. После завершения зависимостей, выполняем текущий узел
        CompletableFuture<AgentResult> currentNodeFuture = allDependencies.thenComposeAsync(v -> {
            Map<String, Object> newContextPayload = new HashMap<>(initialContext.payload());

            // Собираем результаты всех зависимостей и мержим их в контекст
            dependencyFutures.forEach(future -> {
                AgentResult depResult = future.join(); // Безопасно, т.к. allDependencies завершен
                newContextPayload.putAll(depResult.details());
            });

            // Добавляем аргументы текущего узла
            newContextPayload.putAll(node.arguments());

            AgentContext finalContext = new AgentContext(newContextPayload);
            QaAgent agent = toolRegistryService.getAgent(node.agentName())
                    .orElseThrow(() -> new IllegalStateException("Агент не найден: " + node.agentName()));

            log.info("Запуск узла '{}' (агент: {})", node.id(), node.agentName());
            return agent.execute(finalContext);
        });

        // 4. Кэшируем Future для этого узла и возвращаем его
        memo.put(node.id(), currentNodeFuture);
        return currentNodeFuture;
    }
}