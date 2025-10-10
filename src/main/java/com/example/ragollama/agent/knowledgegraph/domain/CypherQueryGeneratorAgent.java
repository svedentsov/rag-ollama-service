package com.example.ragollama.agent.knowledgegraph.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * AI-агент, который переводит вопрос на естественном языке в исполняемый Cypher-запрос.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CypherQueryGeneratorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;

    private static final String GRAPH_SCHEMA = """
            Узлы (Nodes):
            - (:Commit {message: string, author: string, hash: string})
            - (:CodeFile {path: string})
            - (:Method {name: string})
            - (:Requirement {ticketId: string})
            - (:TestCase {path: string})
            
            Связи (Relationships):
            - (:Commit)-[:MODIFIES]->(:Method)
            - (:CodeFile)-[:CONTAINS]->(:Method)
            - (:Commit)-[:IMPLEMENTS]->(:Requirement)
            - (:TestCase)-[:TESTS]->(:CodeFile)
            """;

    @Override
    public String getName() {
        return "cypher-query-generator";
    }

    @Override
    public String getDescription() {
        return "Переводит вопрос на естественном языке в Cypher-запрос к графу знаний.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("question");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String question = (String) context.payload().get("question");

        String promptString = promptService.render("cypherQueryGeneratorPrompt", Map.of(
                "graph_schema", GRAPH_SCHEMA,
                "question", question
        ));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, false)
                .map(cypherQuery -> {
                    String cleanedQuery = cypherQuery.replaceAll("(?i)```cypher|```", "").trim();
                    log.info("Сгенерирован Cypher-запрос для вопроса: '{}'", question);
                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Cypher-запрос успешно сгенерирован.",
                            Map.of("cypherQuery", cleanedQuery)
                    );
                });
    }
}
