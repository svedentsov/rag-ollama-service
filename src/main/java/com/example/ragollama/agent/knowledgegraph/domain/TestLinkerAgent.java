package com.example.ragollama.agent.knowledgegraph.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.git.tools.GitApiClient;
import com.example.ragollama.agent.knowledgegraph.model.GraphNode;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

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
        return context.payload().containsKey("filePath") &&
                ((String) context.payload().get("filePath")).contains("src/test/java");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String testFilePath = (String) context.payload().get("filePath");
        String ref = (String) context.payload().get("ref");

        return gitApiClient.getFileContent(testFilePath, ref)
                .map(content -> {
                    GraphNode testNode = new GraphNode(testFilePath, "TestCase", Map.of("path", testFilePath));
                    graphStorageService.createNode(testNode);

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
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
