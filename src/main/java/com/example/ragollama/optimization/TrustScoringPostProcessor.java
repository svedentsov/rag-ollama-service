package com.example.ragollama.optimization;

import com.example.ragollama.optimization.model.TrustScoreReport;
import com.example.ragollama.rag.postprocessing.RagPostProcessor;
import com.example.ragollama.rag.postprocessing.RagProcessingContext;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.metrics.MetricService;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Асинхронный постпроцессор, вычисляющий многофакторную Оценку Доверия (Trust Score)
 * для каждого сгенерированного RAG-ответа.
 * <p>
 * Этот компонент является ядром системы Explainable AI (XAI), комбинируя
 * детерминированный анализ источников с AI-оценкой уверенности.
 */
@Slf4j
@Component
@Order(40) // Выполняется одним из последних, чтобы иметь полный контекст
@RequiredArgsConstructor
public class TrustScoringPostProcessor implements RagPostProcessor {

    private final SourceAnalyzerService sourceAnalyzer;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final MetricService metricService;
    private final ObjectMapper objectMapper;

    /**
     * Асинхронно выполняет полный цикл вычисления Trust Score.
     *
     * @param context Контекст RAG-взаимодействия.
     * @return {@link CompletableFuture}, который завершается после вычисления и записи метрики.
     */
    @Override
    public CompletableFuture<Void> process(RagProcessingContext context) {
        if (context.documents().isEmpty()) {
            metricService.recordTrustScore(0);
            return CompletableFuture.completedFuture(null);
        }
        // 1. Детерминированный анализ источников
        int recencyScore = sourceAnalyzer.analyzeRecency(context.documents());
        int authorityScore = sourceAnalyzer.analyzeAuthority(context.documents());

        // 2. AI-анализ уверенности
        String contextAsString = context.documents().stream()
                .map(doc -> String.format("<doc source=\"%s\">\n%s\n</doc>",
                        doc.getMetadata().get("source"), doc.getText()))
                .collect(Collectors.joining("\n\n"));

        String promptString = promptService.render("trustScorer", Map.of(
                "context", contextAsString,
                "question", context.originalQuery(),
                "answer", context.response().answer()
        ));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.FAST)
                .thenAccept(llmResponse -> {
                    TrustScoreReport partialReport = parseLlmResponse(llmResponse);
                    // 3. Комбинируем все оценки в финальный скор
                    int finalScore = calculateFinalScore(partialReport.confidenceScore(), recencyScore, authorityScore);
                    TrustScoreReport fullReport = new TrustScoreReport(
                            finalScore, partialReport.confidenceScore(), recencyScore,
                            authorityScore, partialReport.justification()
                    );
                    log.info("Оценка доверия для запроса '{}': {}", context.originalQuery(), fullReport);
                    metricService.recordTrustScore(finalScore);
                }).exceptionally(ex -> {
                    log.error("Ошибка при вычислении Trust Score", ex);
                    return null;
                });
    }

    /**
     * Вычисляет финальную оценку, используя взвешенную формулу.
     *
     * @param confidence Оценка уверенности от AI-критика (0-100).
     * @param recency    Оценка актуальности источников (0-100).
     * @param authority  Оценка авторитетности источников (0-100).
     * @return Композитная оценка доверия (0-100).
     */
    private int calculateFinalScore(int confidence, int recency, int authority) {
        // Веса можно вынести в конфигурацию для гибкого тюнинга
        return (int) (confidence * 0.6 + recency * 0.2 + authority * 0.2);
    }

    /**
     * Безопасно парсит JSON-ответ от LLM.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Десериализованный объект {@link TrustScoreReport}.
     * @throws ProcessingException если парсинг не удался.
     */
    private TrustScoreReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, TrustScoreReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("TrustScorer LLM вернул невалидный JSON.", e);
        }
    }
}
