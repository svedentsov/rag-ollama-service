package com.example.ragollama.agent.buganalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.buganalysis.model.BugAnalysisReport;
import com.example.ragollama.agent.buganalysis.model.BugReportSummary;
import com.example.ragollama.rag.retrieval.HybridRetrievalStrategy;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Агент для поиска дубликатов баг-репортов.
 * <p>
 * Эта версия принимает на вход строго типизированный {@link BugReportSummary},
 * что делает контракт с предыдущим шагом конвейера надежным и явным.
 * Логика парсинга ответа от LLM вынесена в специализированный
 * {@link DuplicateAnalysisParser} для соответствия принципу SRP.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BugDuplicateDetectorAgent implements ToolAgent {

    private final HybridRetrievalStrategy retrievalStrategy;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final DuplicateAnalysisParser duplicateAnalysisParser;

    @Override
    public String getName() {
        return "bug-duplicate-detector";
    }

    @Override
    public String getDescription() {
        return "Ищет семантически похожие баг-репорты и определяет дубликаты.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().get("bugReportSummary") instanceof BugReportSummary;
    }

    /**
     * Асинхронно выполняет полный конвейер анализа на дубликаты.
     *
     * @param context Контекст, содержащий `bugReportSummary`.
     * @return {@link Mono} с результатом, содержащим вердикт и кандидатов.
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        BugReportSummary summary = (BugReportSummary) context.payload().get("bugReportSummary");
        String searchQuery = summary.title();

        return findSimilarBugReports(searchQuery)
                .flatMap(candidateDocs -> {
                    String contextBugs = formatCandidatesForPrompt(candidateDocs);
                    String draftReport = summaryToText(summary);
                    String promptString = promptService.render("bugAnalysisPrompt", Map.of(
                            "draft_report", draftReport,
                            "context_bugs", contextBugs
                    ));
                    return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true);
                })
                .map(tuple -> duplicateAnalysisParser.parse(tuple.getT1()))
                .map(this::buildAgentResult);
    }

    private Mono<List<Document>> findSimilarBugReports(String searchQuery) {
        return retrievalStrategy.retrieve(null, searchQuery, 5, 0.75, null);
    }

    private String formatCandidatesForPrompt(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "Похожих баг-репортов в базе знаний не найдено.";
        }
        return documents.stream()
                .map(doc -> String.format("- ID: %s\n  Текст: %s",
                        doc.getMetadata().get("source"),
                        doc.getText()))
                .collect(Collectors.joining("\n---\n"));
    }

    private String summaryToText(BugReportSummary summary) {
        return String.format(
                "Title: %s\nSteps:\n%s\nExpected: %s\nActual: %s",
                summary.title(), String.join("\n- ", summary.stepsToReproduce()),
                summary.expectedBehavior(), summary.actualBehavior()
        );
    }

    private AgentResult buildAgentResult(BugAnalysisReport analysisResult) {
        String summaryMessage;
        if (analysisResult.isDuplicate()) {
            summaryMessage = String.format("Обнаружен возможный дубликат. Похожие тикеты: %s",
                    analysisResult.duplicateCandidates());
        } else {
            summaryMessage = "Похожих баг-репортов не найдено. Вероятно, тикет уникален.";
        }
        return new AgentResult(
                getName(),
                AgentResult.Status.SUCCESS,
                summaryMessage,
                Map.of(
                        "isDuplicate", analysisResult.isDuplicate(),
                        "candidates", analysisResult.duplicateCandidates(),
                        "bugReportSummary", analysisResult.improvedDescription()
                )
        );
    }
}
