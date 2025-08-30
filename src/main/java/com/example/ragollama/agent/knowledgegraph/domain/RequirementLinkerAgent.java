package com.example.ragollama.agent.knowledgegraph.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.knowledgegraph.domain.GraphStorageService;
import com.example.ragollama.agent.knowledgegraph.model.GraphNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    private final GraphStorageService graphStorageService;
    private static final Pattern TICKET_ID_PATTERN = Pattern.compile("([A-Z]{2,}-\\d+)");

    @Override
    public String getName() {
        return "requirement-linker";
    }

    @Override
    public String getDescription() {
        return "Связывает коммиты с задачами (требованиями, багами) на основе их ID в сообщениях.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        // Зависит от результата CodeParserAgent
        return context.payload().containsKey("codeAnalysis");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            String commitHash = (String) context.payload().get("commitHash");
            String commitMessage = (String) context.payload().get("commitMessage");

            if (commitHash == null || commitMessage == null) {
                return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Нет данных о коммите для анализа.", Map.of());
            }

            Matcher matcher = TICKET_ID_PATTERN.matcher(commitMessage);
            int linksCreated = 0;
            while (matcher.find()) {
                String ticketId = matcher.group(1);
                GraphNode commitNode = new GraphNode(commitHash, "Commit", Map.of());
                GraphNode requirementNode = new GraphNode(ticketId, "Requirement", Map.of("ticketId", ticketId));

                graphStorageService.createNode(requirementNode);
                graphStorageService.createRelationship(commitNode, requirementNode, "IMPLEMENTS");
                linksCreated++;
            }

            String summary = "Анализ ссылок на требования завершен. Создано связей: " + linksCreated;
            log.info(summary);
            return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of());
        });
    }
}
