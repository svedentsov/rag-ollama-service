package com.example.ragollama.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Явная конфигурация для Spring Data JPA.
 * <p>
 * Эта конфигурация указывает Spring Boot сканировать и создавать бины
 * репозиториев JPA исключительно в указанных пакетах (`basePackages`).
 * Это решает проблему конфликта автоконфигурации с другими модулями
 * Spring Data (например, Neo4j) и делает настройку персистентности
 * явной и предсказуемой.
 */
@Configuration
@EnableJpaRepositories(basePackages = {
        "com.example.ragollama.chat.domain",
        "com.example.ragollama.ingestion.domain",
        "com.example.ragollama.monitoring",
        "com.example.ragollama.agent.metrics.domain",
        "com.example.ragollama.agent.dynamic"})
public class JpaPersistenceConfig {
}
