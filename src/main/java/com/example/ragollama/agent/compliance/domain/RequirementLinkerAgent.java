package com.example.ragollama.agent.compliance.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.git.tools.GitApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QA-агент, который связывает коммиты с требованиями или багами.
 * <p>
 * Анализирует сообщения коммитов на предмет наличия идентификаторов задач
 * (например, JIRA-123) и создает соответствующие связи в графе знаний.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequirementLinkerAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private static final Pattern TICKET_ID_PATTERN = Pattern.compile("([A-Z]{2,}-\\d+)");

    @Override
    public String getName() {
        return "requirement-linker";
    }

    @Override
    public String getDescription() {
        return "Анализирует коммиты и связывает их с задачами (требованиями).";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("oldRef") && context.payload().containsKey("newRef");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String oldRef = (String) context.payload().get("oldRef");
        String newRef = (String) context.payload().get("newRef");

        return gitApiClient.getCommitMessages(oldRef, newRef)
                .flatMapMany(Flux::fromIterable)
                .map(commitMessage -> {
                    Matcher matcher = TICKET_ID_PATTERN.matcher(commitMessage);
                    if (matcher.find()) {
                        String commitHash = commitMessage.split(" - ")[0];
                        String ticketId = matcher.group(1);
                        return Map.of("commitHash", commitHash, "ticketId", ticketId, "commitMessage", commitMessage);
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collectList()
                .map(links -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Найдено " + links.size() + " связей между коммитами и задачами.",
                        Map.of("commitToTicketLinks", links)
                ))
                .toFuture();
    }
}
