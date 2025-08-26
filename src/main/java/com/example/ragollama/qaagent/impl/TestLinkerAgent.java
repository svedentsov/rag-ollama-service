package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.domain.GraphStorageService;
import com.example.ragollama.qaagent.model.GraphNode;
import com.example.ragollama.qaagent.tools.GitApiClient;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, который связывает тесты с производственным кодом.
 * <p>
 * Анализирует AST (Abstract Syntax Tree) тестовых файлов для определения,
 * какие классы производственного кода они используют, и создает
 * соответствующие связи `TESTS` в графе знаний.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestLinkerAgent implements ToolAgent {

    private final GraphStorageService graphStorageService;
    private final GitApiClient gitApiClient;
    private final JavaParser javaParser = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));


    @Override
    public String getName() {
        return "test-linker";
    }

    @Override
    public String getDescription() {
        return "Связывает тестовые классы с классами производственного кода на основе анализа импортов.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        // Запускается для измененных тестовых файлов
        return context.payload().containsKey("filePath") &&
                ((String) context.payload().get("filePath")).contains("src/test/java");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String testFilePath = (String) context.payload().get("filePath");
        String ref = (String) context.payload().get("ref");

        return gitApiClient.getFileContent(testFilePath, ref).map(content -> {
            GraphNode testNode = new GraphNode(testFilePath, "TestCase", Map.of("path", testFilePath));
            graphStorageService.createNode(testNode);
            int linksCreated = 0;

            javaParser.parse(content).getResult().ifPresent(cu -> {
                cu.getImports().forEach(imp -> {
                    String importedClass = imp.getNameAsString();
                    if (importedClass.startsWith("com.example.ragollama") && !importedClass.contains(".qaagent.")) {
                        String sourceFilePath = "src/main/java/" + importedClass.replace('.', '/') + ".java";
                        GraphNode sourceNode = new GraphNode(sourceFilePath, "CodeFile", Map.of("path", sourceFilePath));
                        graphStorageService.createNode(sourceNode);
                        graphStorageService.createRelationship(testNode, sourceNode, "TESTS");
                    }
                });
            });
            String summary = "Анализ связей для теста " + testFilePath + " завершен.";
            return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of());
        }).toFuture();
    }
}
