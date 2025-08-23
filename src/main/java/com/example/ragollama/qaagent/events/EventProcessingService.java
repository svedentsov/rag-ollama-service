package com.example.ragollama.qaagent.events;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentOrchestratorService;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.impl.JiraFetcherAgent;
import com.example.ragollama.qaagent.impl.TestPrioritizerAgent;
import com.example.ragollama.qaagent.tools.GitHubApiClient;
import com.example.ragollama.qaagent.tools.JiraApiClient;
import com.example.ragollama.qaagent.web.GitHubPullRequestPayload;
import com.example.ragollama.qaagent.web.JiraIssuePayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Сервис-обработчик событий, который является ядром асинхронного конвейера.
 * <p>
 * Этот сервис вызывается слушателями RabbitMQ ({@link EventConsumerService}).
 * Он отвечает за парсинг входящих сообщений, преобразование их в
 * {@link AgentContext}, взаимодействие с внешними API (GitHub, Jira) для
 * обогащения контекста и запуск соответствующего конвейера агентов через
 * {@link AgentOrchestratorService}. После выполнения конвейера, он форматирует
 * результаты и отправляет их обратно.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessingService {

    private final ObjectMapper objectMapper;
    private final GitHubApiClient gitHubApiClient;
    private final JiraApiClient jiraApiClient;
    private final AgentOrchestratorService agentOrchestratorService;

    /**
     * Обрабатывает событие открытия или синхронизации Pull Request из GitHub.
     * <p>
     * Реализует полный асинхронный конвейер:
     * 1. Десериализует payload.
     * 2. Асинхронно запрашивает diff из GitHub API.
     * 3. Создает контекст и запускает конвейер 'github-pr-pipeline'.
     * 4. Форматирует результат и асинхронно публикует комментарий в PR.
     *
     * @param payloadRaw Сырой JSON payload, полученный из очереди.
     */
    public void processGitHubPullRequestEvent(String payloadRaw) {
        try {
            GitHubPullRequestPayload payload = objectMapper.readValue(payloadRaw, GitHubPullRequestPayload.class);
            String owner = payload.repository().owner().login();
            String repo = payload.repository().name();
            int prNumber = payload.number();

            log.info("Обработка события для PR #{}: action='{}', repo='{}/{}'", prNumber, payload.action(), owner, repo);

            gitHubApiClient.getPullRequestDiff(owner, repo, prNumber)
                    .flatMap(diff -> {
                        AgentContext context = new AgentContext(Map.of(TestPrioritizerAgent.GIT_DIFF_CONTENT_KEY, diff));
                        return Mono.fromFuture(agentOrchestratorService.invokePipeline("github-pr-pipeline", context));
                    })
                    .flatMap(results -> {
                        String comment = formatPipelineResultAsGithubComment(results);
                        return gitHubApiClient.postCommentToPullRequest(owner, repo, prNumber, comment);
                    })
                    .subscribe(
                            v -> log.info("Конвейер обработки для PR #{} успешно завершен.", prNumber),
                            error -> log.error("Ошибка в конвейере обработки события для PR #{}:", prNumber, error)
                    );

        } catch (Exception e) {
            log.error("Не удалось распарсить или обработать GitHub webhook payload", e);
        }
    }

    /**
     * Обрабатывает события, связанные с задачами в Jira.
     * <p>
     * В зависимости от типа события, запускает соответствующий конвейер:
     * <ul>
     *   <li><b>jira:issue_created</b>: Запускает 'jira-bug-creation-pipeline', используя данные из payload.</li>
     *   <li><b>jira:issue_updated</b>: Запускает 'jira-update-analysis-pipeline', который сначала
     *   извлекает актуальные данные через {@link JiraFetcherAgent}.</li>
     * </ul>
     *
     * @param payloadRaw Сырой JSON payload, полученный из очереди.
     */
    public void processJiraIssueEvent(String payloadRaw) {
        try {
            JiraIssuePayload payload = objectMapper.readValue(payloadRaw, JiraIssuePayload.class);
            String eventType = payload.webhookEvent();
            String issueKey = payload.issue().key();

            if ("jira:issue_created".equals(eventType) && "Bug".equals(payload.issue().fields().issuetype().name())) {
                processJiraBugCreation(issueKey, payload);
            } else if ("jira:issue_updated".equals(eventType)) {
                processJiraIssueUpdate(issueKey);
            } else {
                log.trace("Пропускаем необрабатываемое событие Jira: {} для задачи {}", eventType, issueKey);
            }

        } catch (Exception e) {
            log.error("Не удалось распарсить или обработать Jira webhook payload", e);
        }
    }

    /**
     * Обрабатывает создание нового бага.
     */
    private void processJiraBugCreation(String issueKey, JiraIssuePayload payload) {
        log.info("Обработка события создания бага: {}", issueKey);
        String bugText = payload.issue().fields().summary() + "\n" + payload.issue().fields().description();
        AgentContext context = new AgentContext(Map.of("bugReportText", bugText));

        agentOrchestratorService.invokePipeline("jira-bug-creation-pipeline", context)
                .thenCompose(results -> postJiraComment(issueKey, results))
                .whenComplete((result, ex) -> logPipelineCompletion("Jira-задачи " + issueKey, ex));
    }

    /**
     * Обрабатывает обновление существующей задачи.
     */
    private void processJiraIssueUpdate(String issueKey) {
        log.info("Обработка события обновления задачи: {}", issueKey);
        AgentContext context = new AgentContext(Map.of(JiraFetcherAgent.JIRA_ISSUE_KEY, issueKey));

        agentOrchestratorService.invokePipeline("jira-update-analysis-pipeline", context)
                .thenCompose(results -> postJiraComment(issueKey, results))
                .whenComplete((result, ex) -> logPipelineCompletion("Jira-задачи " + issueKey, ex));
    }

    private CompletableFuture<Void> postJiraComment(String issueKey, List<AgentResult> results) {
        if (results.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        String comment = formatPipelineResultAsJiraComment(results);
        return jiraApiClient.postCommentToIssue(issueKey, comment).toFuture();
    }

    private void logPipelineCompletion(String entity, Throwable ex) {
        if (ex != null) {
            log.error("Ошибка в конвейере обработки для {}:", entity, ex.getCause());
        } else {
            log.info("Конвейер обработки для {} успешно завершен.", entity);
        }
    }

    /**
     * Форматирует агрегированный результат работы конвейера в Markdown-комментарий для GitHub.
     *
     * @param results Список результатов от каждого агента в конвейере.
     * @return Строка с отформатированным комментарием.
     */
    private String formatPipelineResultAsGithubComment(List<AgentResult> results) {
        StringBuilder comment = new StringBuilder("### ✅ AI-Ассистент: Результаты анализа\n\n");
        for (AgentResult result : results) {
            comment.append("---\n");
            comment.append("#### Агент: `").append(result.agentName()).append("`\n");
            comment.append("> ").append(result.summary()).append("\n\n");

            if ("test-prioritizer".equals(result.agentName()) && result.details().containsKey("prioritizedTests")) {
                @SuppressWarnings("unchecked")
                List<String> tests = (List<String>) result.details().get("prioritizedTests");
                if (!tests.isEmpty()) {
                    comment.append("**Рекомендуемые тесты для запуска:**\n");
                    comment.append("```\n");
                    tests.forEach(test -> comment.append(test).append("\n"));
                    comment.append("```\n");
                }
            }
        }
        return comment.toString();
    }

    /**
     * Форматирует агрегированный результат работы конвейера в комментарий для Jira.
     *
     * @param results Список результатов от каждого агента в конвейере.
     * @return Строка с отформатированным комментарием.
     */
    private String formatPipelineResultAsJiraComment(List<AgentResult> results) {
        return results.stream()
                .map(result -> String.format("*Агент '%s':* %s", result.agentName(), result.summary()))
                .collect(Collectors.joining("\n"));
    }
}
