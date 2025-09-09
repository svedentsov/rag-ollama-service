package com.example.ragollama.optimization;

import com.example.ragollama.optimization.model.TrustScoreReport;
import com.example.ragollama.rag.domain.model.RagAnswer;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Шаг RAG-конвейера, вычисляющий многофакторную Оценку Доверия (Trust Score)
 * для каждого сгенерированного RAG-ответа.
 */
@Slf4j
@Component
@Order(60) // Выполняется после извлечения цитат (50)
@RequiredArgsConstructor
public class TrustScoringStep implements RagPipelineStep {

    private final SourceAnalyzerService sourceAnalyzer;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final MetricService metricService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [60] Trust Scoring: вычисление оценки доверия...");

        if (context.finalAnswer() == null || context.rerankedDocuments().isEmpty()) {
            metricService.recordTrustScore(0);
            TrustScoreReport emptyReport = new TrustScoreReport(0, 0, 0, 0, "Нет данных для оценки.");
            RagAnswer answer = context.finalAnswer() != null ? context.finalAnswer() : new RagAnswer("", List.of());
            return Mono.just(context.withFinalAnswer(new RagAnswer(answer.answer(), answer.sourceCitations(), emptyReport)));
        }

        // 1. Детерминированный анализ источников
        int recencyScore = sourceAnalyzer.analyzeRecency(context.rerankedDocuments());
        int authorityScore = sourceAnalyzer.analyzeAuthority(context.rerankedDocuments());

        // 2. AI-анализ уверенности
        String contextAsString = context.rerankedDocuments().stream()
                .map(doc -> String.format("<doc source=\"%s\">\n%s\n</doc>",
                        doc.getMetadata().get("source"), doc.getText()))
                .collect(Collectors.joining("\n\n"));

        String promptString = promptService.render("trustScorerPrompt", Map.of(
                "context", contextAsString,
                "question", context.originalQuery(),
                "answer", context.finalAnswer().answer()
        ));

        // Используем надежную модель для получения JSON
        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.FAST_RELIABLE, true))
                .map(llmResponse -> {
                    TrustScoreReport partialReport = parseLlmResponse(llmResponse);
                    // 3. Комбинируем все оценки в финальный скор
                    int finalScore = calculateFinalScore(partialReport.confidenceScore(), recencyScore, authorityScore);
                    TrustScoreReport fullReport = new TrustScoreReport(
                            finalScore, partialReport.confidenceScore(), recencyScore,
                            authorityScore, partialReport.justification()
                    );

                    log.info("Оценка доверия для запроса '{}': {}", context.originalQuery(), fullReport);
                    metricService.recordTrustScore(finalScore);

                    // 4. Обогащаем финальный ответ
                    RagAnswer originalAnswer = context.finalAnswer();
                    RagAnswer answerWithScore = new RagAnswer(originalAnswer.answer(), originalAnswer.sourceCitations(), fullReport);

                    return context.withFinalAnswer(answerWithScore);
                }).onErrorResume(ex -> {
                    log.error("Ошибка при вычислении Trust Score. Устанавливается оценка по умолчанию.", ex);
                    metricService.recordTrustScore(0);
                    TrustScoreReport errorReport = new TrustScoreReport(0, 0, 0, 0, "Ошибка при вычислении оценки.");
                    RagAnswer originalAnswer = context.finalAnswer();
                    RagAnswer answerWithScore = new RagAnswer(originalAnswer.answer(), originalAnswer.sourceCitations(), errorReport);
                    return Mono.just(context.withFinalAnswer(answerWithScore));
                });
    }

    private int calculateFinalScore(int confidence, int recency, int authority) {
        // Веса можно вынести в конфигурацию для гибкого тюнинга
        return (int) (confidence * 0.6 + recency * 0.2 + authority * 0.2);
    }

    private TrustScoreReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, TrustScoreReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("TrustScorer LLM вернул невалидный JSON.", e);
        }
    }
}
