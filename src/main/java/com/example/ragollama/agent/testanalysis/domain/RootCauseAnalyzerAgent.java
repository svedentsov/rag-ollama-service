package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.metrics.model.TestResult;
import com.example.ragollama.agent.metrics.tools.JUnitXmlParser;
import com.example.ragollama.agent.testanalysis.model.RcaResult;
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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * QA-агент для анализа первопричины (Root Cause Analysis) падения тестов.
 * <p>
 * Этот агент является финальным шагом в сложном конвейере. Он собирает
 * все "улики" (стек-трейс, diff кода, логи), подготовленные предыдущими
 * агентами и начальным контекстом, и использует LLM для их комплексного
 * анализа и вынесения вердикта.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RootCauseAnalyzerAgent implements ToolAgent {

    private final JUnitXmlParser jUnitXmlParser;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "root-cause-analyzer";
    }

    @Override
    public String getDescription() {
        return "Анализирует стек-трейсы, diff'ы кода и логи для определения первопричины падения тестов.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("flakyTests")
                && context.payload().containsKey("gitDiffContent")
                && context.payload().containsKey("applicationLogs");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<AgentResult> execute(AgentContext context) {
        List<String> flakyTests = (List<String>) context.payload().get("flakyTests");
        if (flakyTests == null || flakyTests.isEmpty()) {
            return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Падающих тестов для анализа не найдено.", Map.of()));
        }

        String testToAnalyze = flakyTests.getFirst();
        String currentReportContent = (String) context.payload().get("currentTestReportContent");
        String diff = (String) context.payload().get("gitDiffContent");
        String logs = (String) context.payload().get("applicationLogs");

        String stackTrace = jUnitXmlParser.parse(currentReportContent).stream()
                .filter(r -> r.getFullName().equals(testToAnalyze))
                .findFirst()
                .map(TestResult::failureDetails)
                .orElse("Стек-трейс не найден.");

        String promptString = promptService.render("rootCauseAnalysisPrompt", Map.of(
                "failedTest", testToAnalyze,
                "stackTrace", stackTrace,
                "codeDiff", diff.isBlank() ? "Изменений в коде не найдено." : diff,
                "relevantLogs", logs.isBlank() ? "Логи приложения отсутствуют." : logs
        ));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .map(this::parseLlmResponse)
                .map(rcaResult -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        rcaResult.mostLikelyCause(),
                        Map.of("analysisResult", rcaResult)
                ));
    }

    private RcaResult parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, RcaResult.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от RCA LLM: {}", jsonResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON для RCA.", e);
        }
    }
}
