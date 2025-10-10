package com.example.ragollama.agent.architecture.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.git.tools.GitApiClient;
import com.example.ragollama.agent.architecture.model.ComponentNode;
import com.example.ragollama.agent.architecture.model.DependencyLink;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI-агент, который анализирует исходный код и строит граф зависимостей между компонентами.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComponentDependencyExtractorAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final JavaParser javaParser = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "component-dependency-extractor";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Парсит Java-файлы и извлекает зависимости между ними на основе импортов.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("changedFiles") && context.payload().containsKey("newRef");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<AgentResult> execute(AgentContext context) {
        List<String> changedFiles = (List<String>) context.payload().get("changedFiles");
        String newRef = (String) context.payload().get("newRef");

        return Flux.fromIterable(changedFiles)
                .filter(file -> file.endsWith(".java") && file.startsWith("src/main/java"))
                .flatMap(file -> gitApiClient.getFileContent(file, newRef)
                        .map(content -> parseDependencies(file, content)))
                .collectList()
                .map(this::aggregateGraph)
                .map(graph -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Граф зависимостей успешно построен.",
                        Map.of("dependencyGraph", graph)
                ));
    }

    private record ParsedFile(ComponentNode node, List<String> dependencies) {}

    private ParsedFile parseDependencies(String filePath, String content) {
        String className = filePath.substring(filePath.lastIndexOf('/') + 1).replace(".java", "");
        ComponentNode node = new ComponentNode(className, className);

        List<String> dependencies = javaParser.parse(content).getResult()
                .map(cu -> cu.getImports().stream()
                        .map(imp -> imp.getNameAsString())
                        .filter(imp -> imp.startsWith("com.example.ragollama"))
                        .map(imp -> imp.substring(imp.lastIndexOf('.') + 1))
                        .collect(Collectors.toList()))
                .orElse(List.of());

        return new ParsedFile(node, dependencies);
    }

    private Map<String, Object> aggregateGraph(List<ParsedFile> parsedFiles) {
        List<ComponentNode> nodes = parsedFiles.stream().map(ParsedFile::node).toList();
        Map<String, ComponentNode> nodeMap = nodes.stream().collect(Collectors.toMap(ComponentNode::id, n -> n));

        List<DependencyLink> links = parsedFiles.stream()
                .flatMap(pf -> pf.dependencies().stream()
                        .filter(nodeMap::containsKey)
                        .map(dep -> new DependencyLink(pf.node().id(), dep)))
                .distinct()
                .toList();

        return Map.of("nodes", nodes, "links", links);
    }
}
