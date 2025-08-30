package com.example.ragollama.agent.jira.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.jira.tool.JiraApiClient;
import com.example.ragollama.agent.jira.tool.dto.JiraIssueDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, отвечающий за извлечение детальной информации о задаче из Jira API.
 * <p>
 * Этот агент является первым шагом во многих конвейерах, которые начинаются
 * с события, содержащего только идентификатор задачи (например, обновление тикета).
 * Он обогащает {@link AgentContext} данными, необходимыми для последующих
 * аналитических агентов.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JiraFetcherAgent implements ToolAgent {

    public static final String JIRA_ISSUE_KEY = "jiraIssueKey";

    private final JiraApiClient jiraApiClient;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "jira-fetcher";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Извлекает детали задачи (summary, description, status) из Jira по ее ключу.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        Object issueKey = context.payload().get(JIRA_ISSUE_KEY);
        return issueKey instanceof String && !((String) issueKey).isBlank();
    }

    /**
     * Асинхронно выполняет запрос к Jira API для получения деталей задачи.
     *
     * @param context Контекст, содержащий ключ задачи в {@code payload} по ключу {@code JIRA_ISSUE_KEY}.
     * @return {@link CompletableFuture} с результатом, содержащим детали задачи.
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String issueKey = (String) context.payload().get(JIRA_ISSUE_KEY);
        log.info("JiraFetcherAgent: запуск извлечения данных для задачи {}", issueKey);

        return jiraApiClient.getIssueDetails(issueKey)
                .toFuture()
                .thenApply(issueDto -> {
                    JiraIssueDto.Fields fields = issueDto.fields();
                    String bugReportText = fields.summary() + "\n\n" + fields.description();
                    String summary = String.format("Успешно извлечены данные для задачи %s (Статус: %s)",
                            issueKey, fields.status().name());

                    log.info("JiraFetcherAgent: данные для {} успешно извлечены.", issueKey);
                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            summary,
                            Map.of(
                                    "bugReportText", bugReportText,
                                    "issueSummary", fields.summary(),
                                    "issueStatus", fields.status().name()
                            )
                    );
                });
    }
}
