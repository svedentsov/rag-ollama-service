package com.example.ragollama.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Явная конфигурация для Spring Data JPA.
 */
@Configuration
@EnableJpaRepositories(basePackages = {
        "com.example.ragollama.chat.domain",
        "com.example.ragollama.ingestion.domain",
        "com.example.ragollama.monitoring",
        "com.example.ragollama.agent.metrics",
        "com.example.ragollama.agent.dynamic",
        "com.example.ragollama.agent.finops"})
public class JpaPersistenceConfig {
}
