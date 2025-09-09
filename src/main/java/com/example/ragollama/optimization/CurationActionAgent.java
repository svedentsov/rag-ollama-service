package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.jira.domain.JiraTicketCreatorAgent;
import com.example.ragollama.optimization.model.ContradictionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurationActionAgent implements ToolAgent {

    private final JiraTicketCreatorAgent jiraTicketCreatorAgent;

    @Override
    public String getName() {
        return "curation-action-agent";
    }

    @Override
    public String getDescription() {
        return "Создает задачу в Jira на исправление противоречия в базе знаний.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("contradictionDetails");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        ContradictionResult contradiction = (ContradictionResult) context.payload().get("contradictionDetails");
        if (contradiction == null || !contradiction.isContradictory()) {
            return CompletableFuture.completedFuture(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Противоречий нет, задача не создана.", Map.of()));
        }
        Document docA = (Document) context.payload().get("docA");
        Document docB = (Document) context.payload().get("docB");
        String title = "Противоречие в Базе Знаний";
        String description = String.format(
                "AI-хранитель обнаружил потенциальное противоречие в документации между двумя источниками.\n\n" +
                        "*Источник A:* %s (ID чанка: %s)\n" +
                        "*Источник Б:* %s (ID чанка: %s)\n\n" +
                        "h2. Обоснование Противоречия\n" +
                        "%s\n\n" +
                        "Пожалуйста, проверьте и исправьте соответствующие документы для обеспечения консистентности.",
                docA != null ? docA.getMetadata().get("source") : "N/A",
                docA != null ? docA.getMetadata().get("chunkId") : "N/A",
                docB != null ? docB.getMetadata().get("source") : "N/A",
                docB != null ? docB.getMetadata().get("chunkId") : "N/A",
                contradiction.justification()
        );
        AgentContext jiraContext = new AgentContext(Map.of("title", title, "description", description));
        log.info("Создание тикета в Jira для устранения противоречия...");
        return jiraTicketCreatorAgent.execute(jiraContext);
    }
}
