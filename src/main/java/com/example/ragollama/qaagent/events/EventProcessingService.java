package com.example.ragollama.qaagent.events;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentOrchestratorService;
import com.example.ragollama.qaagent.AgentResult;
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
import java.util.stream.Collectors;

/**
 * Сервис-обработчик событий, который является ядром асинхронного конвейера.
 * <p>
 * Этот сервис вызывается слушателями RabbitMQ ({@link EventConsumerService}).
 * Он отвечает за парсинг входящих сообщений, преобразование их в
 * {@link AgentContext} и запуск соответствующего пайплайна агентов через
 * {@link AgentOrchestratorService}. После выполнения пайплайна, он форматирует
 * результаты и отправляет их обратно во внешние системы (GitHub, Jira).
 * <p>
 * Методы этого класса больше не помечены аннотацией `@Async`, так как асинхронность
 * и надежность обеспечиваются на уровне брокера сообщений.
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
                            null,
                            error -> log.error("Ошибка в конвейере обработки события для PR #{}:", prNumber, error)
                    );

        } catch (Exception e) {
            log.error("Не удалось распарсить или обработать GitHub webhook payload", e);
        }
    }

    /**
     * Обрабатывает событие создания задачи в Jira.
     *
     * @param payloadRaw Сырой JSON payload, полученный из очереди.
     */
    public void processJiraIssueEvent(String payloadRaw) {
        try {
            JiraIssuePayload payload = objectMapper.readValue(payloadRaw, JiraIssuePayload.class);
            if (!"jira:issue_created".equals(payload.webhookEvent()) || !"Bug".equals(payload.issue().fields().issuetype().name())) {
                log.trace("Пропускаем событие Jira, не являющееся созданием бага: {}", payload.webhookEvent());
                return;
            }

            String issueKey = payload.issue().key();
            String bugText = payload.issue().fields().summary() + "\n" + payload.issue().fields().description();
            log.info("Обработка события создания бага: {}", issueKey);

            AgentContext context = new AgentContext(Map.of("bugReportText", bugText));

            agentOrchestratorService.invokePipeline("jira-bug-pipeline", context)
                    .thenCompose(results -> {
                        String comment = formatPipelineResultAsJiraComment(results);
                        return jiraApiClient.postCommentToIssue(issueKey, comment).toFuture();
                    })
                    .exceptionally(ex -> {
                        log.error("Ошибка в конвейере обработки события для Jira-задачи {}:", issueKey, ex);
                        return null;
                    });

        } catch (Exception e) {
            log.error("Не удалось распарсить или обработать Jira webhook payload", e);
        }
    }

    /**
     * Форматирует агрегированный результат работы пайплайна в Markdown-комментарий для GitHub.
     *
     * @param results Список результатов от каждого агента в пайплайне.
     * @return Строка с отформатированным комментарием.
     */
    private String formatPipelineResultAsGithubComment(List<AgentResult> results) {
        StringBuilder comment = new StringBuilder("### ✅ AI-Ассистент: Результаты анализа\n\n");
        for (AgentResult result : results) {
            comment.append("--- \n");
            comment.append("#### Агент: `").append(result.agentName()).append("`\n");
            comment.append("> ").append(result.summary()).append("\n\n");
            // Дополнительное форматирование деталей при необходимости
        }
        return comment.toString();
    }

    /**
     * Форматирует агрегированный результат работы пайплайна в комментарий для Jira.
     *
     * @param results Список результатов от каждого агента в пайплайне.
     * @return Строка с отформатированным комментарием.
     */
    private String formatPipelineResultAsJiraComment(List<AgentResult> results) {
        return results.stream()
                .map(result -> String.format("*Агент '%s':* %s", result.agentName(), result.summary()))
                .collect(Collectors.joining("\n"));
    }
}
