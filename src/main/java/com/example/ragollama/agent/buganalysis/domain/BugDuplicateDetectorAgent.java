package com.example.ragollama.agent.buganalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.buganalysis.mappers.BugAnalysisMapper;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Агент для поиска дубликатов баг-репортов, который теперь инкапсулирует всю бизнес-логику.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BugDuplicateDetectorAgent implements ToolAgent {

    private static final String BUG_REPORT_TEXT_KEY = "bugReportText";
    private final HybridRetrievalStrategy retrievalStrategy;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final BugAnalysisMapper bugAnalysisMapper;

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
        return context.payload().containsKey(BUG_REPORT_TEXT_KEY);
    }

    /**
     * Асинхронно выполняет полный конвейер анализа на дубликаты.
     *
     * @param context Контекст, содержащий `bugReportText`.
     * @return {@link CompletableFuture} с результатом, содержащим вердикт и кандидатов.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String bugReportText = (String) context.payload().get(BUG_REPORT_TEXT_KEY);

        return findSimilarBugReports(bugReportText)
                .flatMap(candidateDocs -> {
                    String contextBugs = formatCandidatesForPrompt(candidateDocs);
                    String promptString = promptService.render("bugAnalysisPrompt", Map.of(
                            "draft_report", bugReportText,
                            "context_bugs", contextBugs
                    ));
                    return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED));
                })
                .map(bugAnalysisMapper::parse)
                .map(analysisResponse -> {
                    String summary;
                    if (analysisResponse.isDuplicate()) {
                        summary = String.format("Обнаружен возможный дубликат. Похожие тикеты: %s",
                                analysisResponse.duplicateCandidates());
                    } else {
                        summary = "Похожих баг-репортов не найдено. Вероятно, тикет уникален.";
                    }
                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            summary,
                            Map.of(
                                    "isDuplicate", analysisResponse.isDuplicate(),
                                    "candidates", analysisResponse.duplicateCandidates(),
                                    "improvedDescription", analysisResponse.improvedDescription()
                            )
                    );
                })
                .toFuture();
    }

    private Mono<List<Document>> findSimilarBugReports(String description) {
        return retrievalStrategy.retrieve(null, description, 5, 0.75, null);
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
}
