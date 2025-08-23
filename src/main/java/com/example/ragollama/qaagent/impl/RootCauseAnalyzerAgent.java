package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.QaAgent;
import com.example.ragollama.qaagent.model.RcaResult;
import com.example.ragollama.qaagent.model.TestResult;
import com.example.ragollama.qaagent.tools.GitApiClient;
import com.example.ragollama.qaagent.tools.JUnitXmlParser;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент для анализа первопричины (Root Cause Analysis) падений тестов.
 * <p>
 * Этот агент является финальным шагом в сложном конвейере. Он собирает
 * все "улики" (стек-трейс, diff кода, логи), подготовленные предыдущими
 * агентами и начальным контекстом, и использует LLM для их комплексного
 * анализа и вынесения вердикта.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RootCauseAnalyzerAgent implements QaAgent {

    private final GitApiClient gitApiClient;
    private final JUnitXmlParser jUnitXmlParser;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

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
        // Зависит от всех ключевых данных
        return context.payload().containsKey("flakyTests")
                && context.payload().containsKey("oldRef")
                && context.payload().containsKey("newRef")
                && context.payload().containsKey("applicationLogs");
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        List<String> flakyTests = (List<String>) context.payload().get("flakyTests");
        if (flakyTests == null || flakyTests.isEmpty()) {
            return CompletableFuture.completedFuture(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Падающих тестов для анализа не найдено.", Map.of()));
        }

        // Для простоты анализируем только первый "плавающий" тест
        String testToAnalyze = flakyTests.getFirst();
        String currentReportContent = (String) context.payload().get("currentTestReportContent");
        String oldRef = (String) context.payload().get("oldRef");
        String newRef = (String) context.payload().get("newRef");
        String logs = (String) context.payload().get("applicationLogs");

        // Шаг 1: Получаем детали падения (стек-трейс)
        String stackTrace = jUnitXmlParser.parse(currentReportContent).stream()
                .filter(r -> r.getFullName().equals(testToAnalyze))
                .findFirst()
                .map(TestResult::failureDetails)
                .orElse("Стек-трейс не найден.");

        // Шаг 2: Асинхронно получаем diff кода
        Mono<String> diffMono = gitApiClient.getDiff(oldRef, newRef);

        // Шаг 3: Когда diff получен, запускаем LLM-анализ
        return diffMono.flatMap(diff -> {
                    String promptString = promptService.render("rootCauseAnalysis", Map.of(
                            "failedTest", testToAnalyze,
                            "stackTrace", stackTrace,
                            "codeDiff", diff.isBlank() ? "Изменений в коде не найдено." : diff,
                            "relevantLogs", logs.isBlank() ? "Логи приложения отсутствуют." : logs
                    ));
                    return Mono.fromFuture(llmClient.callChat(new Prompt(promptString)));
                })
                .map(this::parseLlmResponse)
                .map(rcaResult -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        rcaResult.mostLikelyCause(),
                        Map.of("analysisResult", rcaResult)
                ))
                .toFuture();
    }

    private RcaResult parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonResponse.replaceAll("(?s)```json\\s*|\\s*```", "").trim();
            return objectMapper.readValue(cleanedJson, RcaResult.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от RCA LLM: {}", jsonResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON для RCA.", e);
        }
    }
}
