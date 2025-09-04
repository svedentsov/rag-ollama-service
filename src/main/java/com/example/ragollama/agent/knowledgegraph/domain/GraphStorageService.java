package com.example.ragollama.agent.knowledgegraph.domain;

import com.example.ragollama.agent.config.Neo4jProperties;
import com.example.ragollama.agent.knowledgegraph.model.GraphNode;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Низкоуровневый сервис для взаимодействия с графовой базой данных Neo4j.
 * <p>Инкапсулирует всю логику работы с драйвером Neo4j и языком запросов Cypher,
 * предоставляя вышестоящим агентам простые и понятные методы для
 * манипулирования графом.
 */
@Slf4j
@Service
public class GraphStorageService {

    private final Driver driver;

    /**
     * Конструктор, инициализирующий драйвер Neo4j.
     *
     * @param properties Конфигурация для подключения к Neo4j.
     */
    public GraphStorageService(Neo4jProperties properties) {
        this.driver = GraphDatabase.driver(properties.uri(), AuthTokens.basic(properties.username(), properties.password()));
        log.info("Драйвер Neo4j успешно инициализирован.");
    }

    /**
     * Идемпотентно создает узел в графе.
     * <p>Использует оператор `MERGE`, который находит узел по `entityId` или
     * создает новый, если он не существует. Это предотвращает дублирование.
     *
     * @param node DTO с информацией об узле.
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
     *
     * @param fromNode         Начальный узел.
     * @param toNode           Конечный узел.
     * @param relationshipType Тип связи (например, "MODIFIES").
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

    /**
     * Корректно закрывает соединение с базой данных при остановке приложения.
     */
    @PreDestroy
    public void close() {
        driver.close();
        log.info("Соединение с Neo4j успешно закрыто.");
    }
}
