package com.example.ragollama.agent.buganalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Агент для поиска дубликатов баг-репортов.
 * <p>
 * Этот агент является фасадом над {@link BugAnalysisService}. Он извлекает
 * необходимую информацию из {@link AgentContext} и делегирует выполнение
 * сложной логики анализа специализированному сервису, следуя принципам
 * разделения ответственности.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BugDuplicateDetectorAgent implements ToolAgent {

    private static final String BUG_REPORT_TEXT_KEY = "bugReportText";
    private final BugAnalysisService bugAnalysisService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "bug-duplicate-detector";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Ищет семантически похожие баг-репорты и определяет дубликаты.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey(BUG_REPORT_TEXT_KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String bugReportText = (String) context.payload().get(BUG_REPORT_TEXT_KEY);
        return bugAnalysisService.analyzeBugReport(bugReportText)
                .thenApply(analysisResponse -> {
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
                });
    }
}
