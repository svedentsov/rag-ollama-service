package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.model.BugReportSummary;
import com.example.ragollama.qaagent.model.EnhancedBugReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Агент-ассистент, который помогает тестировщику в создании баг-репорта.
 * <p>
 * Этот агент является оркестратором, который использует результаты
 * других, более атомарных агентов (`BugReportSummarizerAgent` и
 * `BugDuplicateDetectorAgent`) для создания единого, обогащенного отчета.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BugReportEnhancerAgent implements ToolAgent {

    @Override
    public String getName() {
        return "bug-report-enhancer";
    }

    @Override
    public String getDescription() {
        return "Обогащает и структурирует 'сырой' баг-репорт, добавляя контекст и ища дубликаты.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("bugReportSummary") && context.payload().containsKey("isDuplicate");
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            BugReportSummary summary = (BugReportSummary) context.payload().get("bugReportSummary");
            boolean isDuplicate = (boolean) context.payload().get("isDuplicate");
            List<String> candidates = (List<String>) context.payload().getOrDefault("candidates", List.of());

            EnhancedBugReport.DuplicateAnalysis duplicateAnalysis =
                    new EnhancedBugReport.DuplicateAnalysis(isDuplicate, candidates);

            EnhancedBugReport finalReport = new EnhancedBugReport(summary, duplicateAnalysis);

            return new AgentResult(
                    getName(),
                    AgentResult.Status.SUCCESS,
                    "Баг-репорт успешно обогащен и проанализирован.",
                    Map.of("enhancedBugReport", finalReport)
            );
        });
    }
}
