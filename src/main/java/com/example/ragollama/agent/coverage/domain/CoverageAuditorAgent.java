package com.example.ragollama.agent.coverage.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.coverage.model.FileCoverageRisk;
import com.example.ragollama.agent.coverage.tool.JacocoReportParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * QA-агент, который выполняет аудит тестового покрытия для измененных файлов.
 * <p>
 * Сопоставляет список измененных файлов с данными из отчета о покрытии
 * и классифицирует каждый файл по уровню риска.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoverageAuditorAgent implements ToolAgent {

    private final JacocoReportParser jacocoReportParser;

    @Override
    public String getName() {
        return "coverage-auditor";
    }

    @Override
    public String getDescription() {
        return "Анализирует измененные файлы и их тестовое покрытие для определения уровня риска.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("changedFiles") && context.payload().containsKey("jacocoReportContent");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<AgentResult> execute(AgentContext context) {
        return Mono.fromCallable(() -> {
            List<String> changedFiles = (List<String>) context.payload().get("changedFiles");
            String jacocoReport = (String) context.payload().get("jacocoReportContent");
            Map<String, Double> coverageData = jacocoReportParser.parse(jacocoReport);

            List<FileCoverageRisk> risks = changedFiles.stream()
                    .filter(file -> file.startsWith("src/main/java")) // Анализируем только исходный код
                    .map(file -> {
                        double coverage = coverageData.getOrDefault(file, 0.0);
                        FileCoverageRisk.RiskLevel riskLevel;
                        String summary;

                        if (coverage < 40.0) {
                            riskLevel = FileCoverageRisk.RiskLevel.HIGH;
                            summary = "Критически низкое покрытие.";
                        } else if (coverage < 80.0) {
                            riskLevel = FileCoverageRisk.RiskLevel.MEDIUM;
                            summary = "Недостаточное покрытие.";
                        } else {
                            riskLevel = FileCoverageRisk.RiskLevel.LOW;
                            summary = "Хорошее покрытие.";
                        }
                        return new FileCoverageRisk(file, coverage, riskLevel, summary);
                    })
                    .toList();

            String summary = "Аудит покрытия завершен. Проанализировано " + risks.size() + " измененных файлов.";
            return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("coverageRisks", risks));
        });
    }
}
