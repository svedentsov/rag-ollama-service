package com.example.ragollama.optimization;

import com.example.ragollama.agent.knowledgegraph.domain.GraphQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GraphContextExpanderService {

    private final GraphQueryService graphQueryService;
    private final ObjectMapper objectMapper;

    /**
     * Расширяет список документов, добавляя к ним связанные сущности из графа.
     *
     * @param initialDocs Исходный список документов из поиска.
     * @return Mono со списком, содержащим исходные и добавленные из графа документы.
     */
    public Mono<List<Document>> expand(List<Document> initialDocs) {
        if (initialDocs.isEmpty()) {
            return Mono.just(List.of());
        }

        return Flux.fromIterable(initialDocs)
                .flatMap(this::findRelatedNodes)
                .collectList()
                .map(relatedDocs -> {
                    List<Document> combined = new ArrayList<>(initialDocs);
                    combined.addAll(relatedDocs);
                    List<Document> distinct = combined.stream()
                            .filter(doc -> doc.getMetadata().get("chunkId") != null)
                            .collect(Collectors.toMap(
                                    doc -> doc.getMetadata().get("chunkId").toString(),
                                    doc -> doc,
                                    (doc1, doc2) -> doc1))
                            .values().stream().toList();
                    log.info("Граф знаний расширил контекст с {} до {} документов.", initialDocs.size(), distinct.size());
                    return distinct;
                });
    }

    private Flux<Document> findRelatedNodes(Document doc) {
        String docType = (String) doc.getMetadata().get("doc_type");
        if (!"test_case".equals(docType)) {
            return Flux.empty(); // Пока расширяем контекст только для тест-кейсов
        }

        String testCasePath = (String) doc.getMetadata().get("documentId");
        if (testCasePath == null) {
            return Flux.empty();
        }

        String cypher = String.format(
                "MATCH (t:TestCase {entityId: '%s'})-[:TESTS]->(f:CodeFile) " +
                        "OPTIONAL MATCH (f)<-[:CONTAINS]-(m:Method)<-[:MODIFIES]-(c:Commit)-[:IMPLEMENTS]->(r:Requirement) " +
                        "RETURN f.path as sourceFile, r.ticketId as requirementId", testCasePath.replace("'", "''"));

        return Mono.fromCallable(() -> graphQueryService.executeQuery(cypher))
                .flatMapMany(Flux::fromIterable)
                .map(this::convertGraphResultToDocument);
    }

    private Document convertGraphResultToDocument(Map<String, Object> graphRow) {
        try {
            String content = "Связанная сущность из графа знаний: " + objectMapper.writeValueAsString(graphRow);
            Map<String, Object> metadata = Map.of(
                    "source", "KnowledgeGraph",
                    "doc_type", "graph_relation",
                    "chunkId", "graph-" + graphRow.hashCode() // Простой способ генерации ID
            );
            return new Document(content, metadata);
        } catch (JsonProcessingException e) {
            log.warn("Не удалось преобразовать результат графа в JSON", e);
            return null;
        }
    }
}
