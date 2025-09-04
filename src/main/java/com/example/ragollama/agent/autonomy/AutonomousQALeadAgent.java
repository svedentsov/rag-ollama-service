package com.example.ragollama.agent.autonomy;

import com.example.ragollama.agent.AgentContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Автономный "мета-агент", выступающий в роли AI QA Lead.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutonomousQALeadAgent {

    private final AutonomousGoalExecutor goalExecutor;

    /**
     * Запускает полный цикл автономного анализа и планирования по расписанию.
     * Расписание настраивается в {@code application.yml} через свойство
     * {@code app.analysis.autonomy.cron}.
     */
    @Scheduled(cron = "${app.analysis.autonomy.cron:0 0 7 * * MON-FRI}") // По умолчанию - в 7 утра по будням
    public void runAutonomousAnalysis() {
        log.info("Запуск планового автономного анализа состояния проекта...");
        final String goal = "Провести полный анализ технического долга в тестах и паттернов багов за последние 7 дней. " +
                "На основе анализа, найти самые критичные проблемы и создать для них задачи в Jira.";
        final AgentContext context = new AgentContext(Map.of("analysisPeriodDays", 7, "days", 7));
        goalExecutor.executeGoal(goal, context);
    }
}
