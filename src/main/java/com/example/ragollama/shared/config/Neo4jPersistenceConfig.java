package com.example.ragollama.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

/**
 * Явная конфигурация для Spring Data Neo4j.
 * <p>
 * Эта конфигурация указывает Spring Boot сканировать и создавать бины
 * репозиториев Neo4j исключительно в указанном пакете. Это предотвращает
 * попытки Spring анализировать JPA-репозитории как кандидатов для Neo4j,
 * устраняя "шум" в логах при запуске и делая архитектуру чище.
 * <p>
 * На данный момент в указанном пакете нет репозиториев, но эта конфигурация
 * закладывает правильный фундамент для их добавления в будущем.
 */
@Configuration
@EnableNeo4jRepositories(basePackages = "com.example.ragollama.agent.knowledgegraph.repository")
public class Neo4jPersistenceConfig {
}
