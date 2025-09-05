package com.example.ragollama.agent.architecture.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.architecture.model.ComponentNode;
import com.example.ragollama.agent.architecture.model.DependencyLink;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Агент-инструмент, который преобразует структурированное представление графа
 * в синтаксис диаграмм Mermaid.js.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MermaidDiagramGeneratorAgent implements ToolAgent {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "mermaid-diagram-generator";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Генерирует код диаграммы Mermaid.js из структурированного графа зависимостей.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("dependencyGraph");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> graph = (Map<String, Object>) context.payload().get("dependencyGraph");
            List<ComponentNode> nodes = (List<ComponentNode>) graph.get("nodes");
            List<DependencyLink> links = (List<DependencyLink>) graph.get("links");

            StringBuilder mermaidBuilder = new StringBuilder("graph TD;\n");
            nodes.forEach(node -> mermaidBuilder.append(String.format("    %s[\"%s\"];\n", node.id(), node.label())));
            links.forEach(link -> mermaidBuilder.append(String.format("    %s --> %s;\n", link.sourceId(), link.targetId())));

            String mermaidCode = mermaidBuilder.toString();

            return new AgentResult(
                    getName(),
                    AgentResult.Status.SUCCESS,
                    "Код диаграммы Mermaid.js успешно сгенерирован.",
                    Map.of("mermaidCode", mermaidCode)
            );
        });
    }
}
