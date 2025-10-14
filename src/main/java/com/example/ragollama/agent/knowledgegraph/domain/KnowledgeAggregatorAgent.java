package com.example.ragollama.agent.knowledgegraph.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.knowledgegraph.model.KnowledgeGraphResponse;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * Мета-агент "Агрегатор Знаний".
 *
 * <p>Является финальным шагом в конвейере. Он принимает "сырые" данные,
 * полученные из графа знаний, и использует LLM для их интерпретации и
 * синтеза человекочитаемого ответа на исходный вопрос пользователя.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeAggregatorAgent implements ToolAgent {

    private final GraphQueryService graphQueryService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "knowledge-aggregator";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Интерпретирует результаты Cypher-запроса и генерирует ответ на естественном языке.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("cypherQuery");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String question = (String) context.payload().get("question");
        String cypherQuery = (String) context.payload().get("cypherQuery");

        return Mono.fromCallable(() -> graphQueryService.executeQuery(cypherQuery))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(graphResult -> {
                    try {
                        String graphResultJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(graphResult);
                        String promptString = promptService.render("knowledgeAggregator", Map.of(
                                "question", question,
                                "cypher_query", cypherQuery,
                                "graph_result_json", graphResultJson
                        ));

                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                                .map(tuple -> {
                                    String naturalLanguageResponse = tuple.getT1();
                                    KnowledgeGraphResponse finalResponse = new KnowledgeGraphResponse(
                                            naturalLanguageResponse,
                                            cypherQuery,
                                            graphResult
                                    );
                                    return new AgentResult(
                                            getName(),
                                            AgentResult.Status.SUCCESS,
                                            "Ответ на основе графа знаний успешно сгенерирован.",
                                            Map.of("knowledgeGraphResponse", finalResponse)
                                    );
                                });
                    } catch (JsonProcessingException e) {
                        return Mono.error(new ProcessingException("Ошибка сериализации результата графа.", e));
                    }
                });
    }
}
