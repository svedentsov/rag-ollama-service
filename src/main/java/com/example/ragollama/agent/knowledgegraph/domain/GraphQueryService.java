package com.example.ragollama.agent.knowledgegraph.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис для выполнения Cypher-запросов к графовой базе данных Neo4j.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphQueryService {

    private final Driver driver;

    /**
     * Выполняет предоставленный Cypher-запрос и возвращает результат.
     *
     * @param cypherQuery Cypher-запрос для выполнения.
     * @return Список карт, где каждая карта представляет одну строку результата.
     */
    public List<Map<String, Object>> executeQuery(String cypherQuery) {
        log.info("Выполнение Cypher-запроса: {}", cypherQuery);
        try (Session session = driver.session()) {
            Result result = session.run(cypherQuery);
            return result.stream()
                    .map(record -> record.asMap())
                    .collect(Collectors.toList());
        }
    }
}
