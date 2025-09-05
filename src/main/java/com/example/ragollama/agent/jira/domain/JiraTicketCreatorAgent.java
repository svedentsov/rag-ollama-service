package com.example.ragollama.agent.jira.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.jira.tool.JiraApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, выполняющий "действие" - создание тикета в Jira.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JiraTicketCreatorAgent implements ToolAgent {

    private final JiraApiClient jiraApiClient;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "jira-ticket-creator";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Создает новый тикет в Jira с заданными параметрами (заголовок, описание, метки).";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("title") && context.payload().containsKey("description");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Создание тикетов во внешней системе является рискованной операцией,
     * которая может привести к "спаму". Поэтому она требует явного
     * утверждения человеком через механизм Human-in-the-Loop.
     *
     * @return всегда {@code true}.
     */
    @Override
    public boolean requiresApproval() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String title = (String) context.payload().get("title");
        String description = (String) context.payload().get("description");
        log.info("Запрос на создание тикета в Jira: {}", title);
        // ВАЖНО: В реальной системе здесь будет вызов jiraApiClient.createIssue(...)
        // Для демонстрации возвращаем mock-результат.
        return CompletableFuture.supplyAsync(() -> {
            String mockIssueKey = "PROJ-" + (new java.util.Random().nextInt(900) + 100);
            String summary = "Тикет '" + title + "' успешно создан с ключом " + mockIssueKey;
            log.info(summary);
            return new AgentResult(
                    getName(),
                    AgentResult.Status.SUCCESS,
                    summary,
                    Map.of("issueKey", mockIssueKey)
            );
        });
    }
}
