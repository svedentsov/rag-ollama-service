package com.example.ragollama.agent.knowledgegraph.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.knowledgegraph.model.CodeAnalysisResult;
import com.example.ragollama.agent.knowledgegraph.model.GraphNode;
import com.example.ragollama.agent.knowledgegraph.model.MethodDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, который строит подграф знаний на основе структурного анализа кода.
 * <p>Этот агент является "строителем". Он принимает на вход результат работы
 * {@link CodeParserAgent} и преобразует его в узлы и связи в графовой базе данных.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeGraphBuilderAgent implements ToolAgent {

    private final GraphStorageService graphStorageService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "code-graph-builder";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Обновляет граф знаний узлами и связями на основе анализа кода (файлы, методы, коммиты).";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("codeAnalysis");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            CodeAnalysisResult analysisResult = (CodeAnalysisResult) context.payload().get("codeAnalysis");
            String filePath = analysisResult.filePath();

            GraphNode fileNode = new GraphNode(filePath, "CodeFile", Map.of("path", filePath));
            graphStorageService.createNode(fileNode);

            int methodsProcessed = 0;
            for (MethodDetails method : analysisResult.methods()) {
                // Создаем узел для метода
                String methodId = filePath + "#" + method.methodName();
                GraphNode methodNode = new GraphNode(methodId, "Method", Map.of("name", method.methodName(), "startLine", method.startLine()));
                graphStorageService.createNode(methodNode);
                graphStorageService.createRelationship(fileNode, methodNode, "CONTAINS");

                // Создаем узел для коммита и связь
                if (method.lastCommitInfo() != null) {
                    var commitInfo = method.lastCommitInfo();
                    GraphNode commitNode = new GraphNode(commitInfo.commitHash(), "Commit", Map.of(
                            "message", commitInfo.commitMessage(),
                            "author", commitInfo.authorName(),
                            "timestamp", commitInfo.commitTime().toString()
                    ));
                    graphStorageService.createNode(commitNode);
                    graphStorageService.createRelationship(commitNode, methodNode, "MODIFIES");
                }
                methodsProcessed++;
            }

            String summary = String.format("Граф знаний обновлен для файла %s. Обработано %d методов.", filePath, methodsProcessed);
            log.info(summary);
            return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of());
        });
    }
}
