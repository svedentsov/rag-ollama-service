package com.example.ragollama.agent.performance.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.git.tools.GitApiClient;
import com.example.ragollama.agent.performance.model.PerformanceBottleneckReport;
import com.example.ragollama.agent.performance.model.PerformanceFinding;
import com.example.ragollama.agent.performance.tool.PerformanceAntiPatternDetector;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI-агент, который анализирует код на предмет потенциальных узких мест в производительности.
 * <p>
 * Этот агент реализует гибридный подход:
 * 1. Детерминированный AST-анализатор (`PerformanceAntiPatternDetector`) находит известные
 * анти-паттерны (например, N+1 запросы).
 * 2. LLM используется для экспертной оценки найденных проблем, объяснения их влияния
 * и предложения конкретных вариантов исправления кода.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PerformanceBottleneckFinderAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final PerformanceAntiPatternDetector antiPatternDetector;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "performance-bottleneck-finder";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует код на известные анти-паттерны производительности (например, N+1 запросы).";
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
                        .flatMap(content -> analyzeFileForBottlenecks(file, content))
                )
                .collectList()
                .map(allFindings -> {
                    List<PerformanceFinding> flattenedFindings = allFindings.stream()
                            .flatMap(List::stream)
                            .collect(Collectors.toList());

                    PerformanceBottleneckReport report = new PerformanceBottleneckReport(flattenedFindings);
                    String summary = "Анализ производительности завершен. Найдено потенциальных проблем: " + flattenedFindings.size();
                    return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("performanceReport", report));
                });
    }

    private Mono<List<PerformanceFinding>> analyzeFileForBottlenecks(String filePath, String code) {
        List<PerformanceAntiPatternDetector.AntiPatternOccurrence> antiPatterns = antiPatternDetector.detectDbCallsInLoops(code);
        if (antiPatterns.isEmpty()) {
            return Mono.just(List.of());
        }

        return Flux.fromIterable(antiPatterns)
                .flatMap(pattern -> {
                    String promptString = promptService.render("performanceBottleneckPrompt", Map.of(
                            "filePath", filePath,
                            "antiPatternType", pattern.getType(),
                            "codeSnippet", pattern.getCodeSnippet()
                    ));
                    return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                            .map(this::parseLlmResponse);
                })
                .collectList();
    }

    private PerformanceFinding parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, PerformanceFinding.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("LLM вернула невалидный JSON для отчета о производительности.", e);
        }
    }
}
