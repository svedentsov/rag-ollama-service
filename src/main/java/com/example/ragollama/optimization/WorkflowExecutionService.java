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

    private CompletableFuture<AgentResult> executeNode(
            WorkflowNode node,
            Map<String, WorkflowNode> nodeMap,
            Map<String, CompletableFuture<AgentResult>> memo,
            AgentContext initialContext) {
        if (memo.containsKey(node.id())) {
            return memo.get(node.id());
        }
        List<CompletableFuture<AgentResult>> dependencyFutures = node.dependencies().stream()
                .map(depId -> nodeMap.get(depId))
                .map(depNode -> executeNode(depNode, nodeMap, memo, initialContext))
                .toList();
        CompletableFuture<Void> allDependencies = CompletableFuture.allOf(dependencyFutures.toArray(new CompletableFuture[0]));
        CompletableFuture<AgentResult> currentNodeFuture = allDependencies.thenComposeAsync(v -> {
            Map<String, Object> newContextPayload = new HashMap<>(initialContext.payload());
            dependencyFutures.forEach(future -> {
                AgentResult depResult = future.join();
                newContextPayload.putAll(depResult.details());
            });
            newContextPayload.putAll(node.arguments());
            AgentContext finalContext = new AgentContext(newContextPayload);
            QaAgent agent = toolRegistryService.getAgent(node.agentName())
                    .orElseThrow(() -> new IllegalStateException("Агент не найден: " + node.agentName()));
            log.info("Запуск узла '{}' (агент: {})", node.id(), node.agentName());
            return agent.execute(finalContext);
        });
        memo.put(node.id(), currentNodeFuture);
        return currentNodeFuture;
    }
}