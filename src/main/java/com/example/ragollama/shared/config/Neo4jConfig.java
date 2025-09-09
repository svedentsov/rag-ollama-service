package com.example.ragollama.shared.config;

import com.example.ragollama.agent.config.Neo4jProperties;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class Neo4jConfig {

    /**
     * Создает единственный, централизованный бин Neo4j Driver для всего приложения.
     * Этот бин будет автоматически внедряться во все сервисы, которым он необходим.
     *
     * @param properties Типобезопасная конфигурация с учетными данными.
     * @return Сконфигурированный экземпляр драйвера.
     */
    @Bean(destroyMethod = "close")
    public Driver neo4jDriver(Neo4jProperties properties) {
        log.info("Конфигурирование бина Neo4j Driver для URI: {}", properties.uri());
        return GraphDatabase.driver(
                properties.uri(),
                AuthTokens.basic(properties.username(), properties.password())
        );
    }
}
