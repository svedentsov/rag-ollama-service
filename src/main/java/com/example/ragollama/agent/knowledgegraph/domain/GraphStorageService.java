package com.example.ragollama.agent.knowledgegraph.domain;

import com.example.ragollama.agent.knowledgegraph.model.GraphNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Низкоуровневый сервис для взаимодействия с графовой базой данных Neo4j.
 * Инкапсулирует всю логику работы с драйвером Neo4j и языком запросов Cypher.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphStorageService {

    private final Driver driver;

    /**
     * Идемпотентно создает узел в графе.
     */
    public void createNode(GraphNode node) {
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                String cypher = String.format("MERGE (n:%s {entityId: $entityId}) SET n += $properties", node.type());
                tx.run(cypher, Map.of("entityId", node.entityId(), "properties", node.properties()));
                return null;
            });
            log.debug("Узел типа '{}' с ID '{}' успешно создан или обновлен.", node.type(), node.entityId());
        }
    }

    /**
     * Создает направленную связь между двумя существующими узлами.
     */
    public void createRelationship(GraphNode fromNode, GraphNode toNode, String relationshipType) {
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                String cypher = String.format(
                        "MATCH (a:%s {entityId: $fromId}), (b:%s {entityId: $toId}) " +
                                "MERGE (a)-[r:%s]->(b)",
                        fromNode.type(), toNode.type(), relationshipType);
                tx.run(cypher, Map.of("fromId", fromNode.entityId(), "toId", toNode.entityId()));
                return null;
            });
            log.debug("Связь '[{}]' от '{}' к '{}' успешно создана.", relationshipType, fromNode.entityId(), toNode.entityId());
        }
    }
}
