package com.example.ragollama.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Явная конфигурация для Spring Data JPA.
 * <p>
 * Эта аннотация отключает автоматическое сканирование всего classpath
 * и требует явного перечисления пакетов, содержащих JPA-репозитории.
 * Такой подход повышает производительность и предсказуемость, делая
 * архитектуру более строгой и самодокументируемой.
 * <p>
 * При добавлении репозиториев в новые пакеты необходимо обновить этот список.
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
