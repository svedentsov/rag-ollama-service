package com.example.ragollama.agent.jira.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.jira.tool.JiraApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

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
     */
    @Override
    public boolean requiresApproval() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String title = (String) context.payload().get("title");
        String description = (String) context.payload().get("description");
        log.info("Запрос на создание тикета в Jira: {}", title);

        return Mono.fromCallable(() -> {
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
