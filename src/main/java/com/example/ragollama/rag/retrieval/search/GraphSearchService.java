package com.example.ragollama.rag.retrieval.search;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.knowledgegraph.domain.CypherQueryGeneratorAgent;
import com.example.ragollama.agent.knowledgegraph.domain.GraphQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис для выполнения поиска в графе знаний.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GraphSearchService {

    private final CypherQueryGeneratorAgent cypherQueryGeneratorAgent;
    private final GraphQueryService graphQueryService;
    private final ObjectMapper objectMapper;

    /**
     * Выполняет поиск в графе на основе вопроса на естественном языке.
     *
     * @param query Вопрос пользователя.
     * @return {@link Mono} со списком документов, представляющих результаты.
     */
    public Mono<List<Document>> search(String query) {
        return cypherQueryGeneratorAgent.execute(new AgentContext(Map.of("question", query)))
                .map(agentResult -> (String) agentResult.details().get("cypherQuery"))
                .flatMap(cypherQuery -> Mono.fromCallable(() -> graphQueryService.executeQuery(cypherQuery)))
                .map(this::convertGraphResultsToDocuments);
    }

    private List<Document> convertGraphResultsToDocuments(List<Map<String, Object>> graphResults) {
        return graphResults.stream()
                .map(row -> {
                    try {
                        String content = objectMapper.writeValueAsString(row);
                        Map<String, Object> metadata = Map.of(
                                "source", "KnowledgeGraph",
                                "doc_type", "graph_result",
                                "chunkId", "graph-" + row.hashCode()
                        );
                        return new Document(content, metadata);
                    } catch (JsonProcessingException e) {
                        log.warn("Не удалось преобразовать результат графа в JSON", e);
                        return null;
                    }
                })
                .filter(doc -> doc != null)
                .collect(Collectors.toList());
    }
}
