package com.example.ragollama.agent.events;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.dynamic.DynamicPipelineExecutionService;
import com.example.ragollama.agent.dynamic.PlanningAgentService;
import com.example.ragollama.agent.events.dto.GitHubPullRequestPayload;
import com.example.ragollama.agent.events.dto.JiraIssuePayload;
import com.example.ragollama.agent.git.tool.GitHubApiClient;
import com.example.ragollama.agent.jira.domain.JiraFetcherAgent;
import com.example.ragollama.agent.jira.tool.JiraApiClient;
import com.example.ragollama.agent.testanalysis.domain.TestPrioritizerAgent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Сервис-обработчик, который является ядром асинхронного конвейера.
 * <p>
 * Все публичные методы этого сервиса аннотированы {@code @Async}, что
 * заставляет Spring выполнять их в отдельном потоке из пула `applicationTaskExecutor`.
 * Это позволяет вызывающим компонентам (например, {@link com.example.ragollama.agent.events.api.WebhookController})
 * немедленно вернуть ответ, не дожидаясь завершения долгой операции.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessingService {

    private final ObjectMapper objectMapper;
    private final GitHubApiClient gitHubApiClient;
    private final JiraApiClient jiraApiClient;
    private final AgentOrchestratorService agentOrchestratorService;
    private final PlanningAgentService planningAgentService;
    private final DynamicPipelineExecutionService executionService;

    /**
     * Асинхронно обрабатывает любое событие от GitHub.
     *
     * @param eventType  Тип события (например, "pull_request").
     * @param payloadRaw Сырой JSON payload.
     */
    @Async("applicationTaskExecutor")
    public void processGitHubEvent(String eventType, String payloadRaw) {
        switch (eventType) {
            case "pull_request" -> processGitHubPullRequestEvent(payloadRaw);
            case "push" -> processGitHubPushEvent(payloadRaw);
            default -> log.trace("Пропускаем необрабатываемое событие GitHub: {}", eventType);
        }
    }

    /**
     * Обрабатывает событие открытия или синхронизации Pull Request из GitHub.
     *
     * @param payloadRaw Сырой JSON payload.
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
                        return Mono.fromFuture(agentOrchestratorService.invoke("github-pr-pipeline", context));
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
            log.error("Не удалось распарсить или обработать GitHub PR webhook payload", e);
        }
    }

    /**
     * Обрабатывает событие push в репозиторий GitHub.
     *
     * @param payloadRaw Сырой JSON payload.
     */
    public void processGitHubPushEvent(String payloadRaw) {
        try {
            JsonNode payload = objectMapper.readTree(payloadRaw);
            String ref = payload.path("ref").asText();
            String afterCommit = payload.path("after").asText();
            String beforeCommit = payload.path("before").asText();

            if (!"refs/heads/main".equals(ref) && !"refs/heads/develop".equals(ref)) {
                log.info("Push в ветку '{}', не требующую запуска тестов. Пропуск.", ref);
                return;
            }

            log.info("Обнаружен push в ветку '{}'. Инициирование планирования тестов...", ref);

            String taskDescription = String.format(
                    "Изменения были отправлены в ветку '%s'. Проанализируй эти изменения и спланируй запуск регрессионных тестов.", ref);

            Map<String, Object> initialContext = Map.of(
                    "oldRef", beforeCommit,
                    "newRef", afterCommit,
                    "jobName", "regression-test-suite"
            );

            planningAgentService.createPlan(taskDescription, initialContext)
                    .flatMap(plan -> executionService.executePlan(plan, new AgentContext(initialContext), null))
                    .subscribe(
                            results -> log.info("Конвейер планирования тестов для push в '{}' успешно завершен.", ref),
                            error -> log.error("Ошибка в конвейере планирования тестов:", error)
                    );
        } catch (Exception e) {
            log.error("Не удалось обработать GitHub push webhook payload", e);
        }
    }

    /**
     * Асинхронно обрабатывает любое событие от Jira.
     *
     * @param payloadRaw Сырой JSON payload.
     */
    @Async("applicationTaskExecutor")
    public void processJiraEvent(String payloadRaw) {
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

        agentOrchestratorService.invoke("jira-bug-creation-pipeline", context)
                .thenCompose(results -> postJiraComment(issueKey, results))
                .whenComplete((result, ex) -> logPipelineCompletion("Jira-задачи " + issueKey, ex));
    }

    /**
     * Обрабатывает обновление существующей задачи.
     */
    private void processJiraIssueUpdate(String issueKey) {
        log.info("Обработка события обновления задачи: {}", issueKey);
        AgentContext context = new AgentContext(Map.of(JiraFetcherAgent.JIRA_ISSUE_KEY, issueKey));

        agentOrchestratorService.invoke("jira-update-analysis-pipeline", context)
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
