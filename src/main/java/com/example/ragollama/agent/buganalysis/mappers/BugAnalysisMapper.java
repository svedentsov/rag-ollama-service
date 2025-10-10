package com.example.ragollama.agent.buganalysis.mappers;

import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.buganalysis.model.BugAnalysisReport;
import com.example.ragollama.agent.buganalysis.model.BugReportSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Компонент-маппер, отвечающий за преобразование внутреннего результата
 * работы конвейера ({@link AgentResult}) в публичный DTO ответа ({@link BugAnalysisReport}).
 * <p>
 * Является "антикоррупционным слоем", изолирующим публичный API от деталей реализации агентов.
 */
@Component
@Slf4j
public class BugAnalysisMapper {

    /**
     * Преобразует результат работы конвейера в публичный DTO.
     *
     * @param results Список результатов от агентов конвейера.
     * @return DTO {@link BugAnalysisReport} для API.
     * @throws IllegalStateException если результат не содержит ожидаемых данных.
     */
    @SuppressWarnings("unchecked")
    public BugAnalysisReport toReport(List<AgentResult> results) {
        if (results == null || results.isEmpty()) {
            throw new IllegalStateException("Конвейер анализа багов не вернул результат.");
        }

        // Берем результат последнего агента в цепочке, который содержит агрегированные данные
        AgentResult finalResult = results.getLast();
        var details = finalResult.details();

        BugReportSummary summary = (BugReportSummary) details.get("bugReportSummary");
        boolean isDuplicate = (boolean) details.getOrDefault("isDuplicate", false);
        List<String> candidates = (List<String>) details.getOrDefault("candidates", List.of());

        if (summary == null) {
            throw new IllegalStateException("Результат конвейера не содержит обязательный 'bugReportSummary'.");
        }

        return new BugAnalysisReport(summary, isDuplicate, candidates);
    }
}
