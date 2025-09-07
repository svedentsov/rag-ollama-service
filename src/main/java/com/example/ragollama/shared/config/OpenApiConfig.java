// backend/src/main/java/com/example/ragollama/shared/config/OpenApiConfig.java
package com.example.ragollama.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация для OpenAPI (Swagger) документации.
 * *
 * <p>Эта версия была упрощена путем удаления конфигурации безопасности.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Создает и настраивает главный бин {@link OpenAPI}.
     *
     * @return Настроенный экземпляр {@link OpenAPI}.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RAG Ollama Service API")
                        .version("1.0.0")
                        .description("""
                                Микросервис, демонстрирующий интеграцию Spring AI с локальной LLM (Ollama)
                                и реализацию архитектуры Retrieval-Augmented Generation (RAG)
                                с использованием векторной базы данных PgVector.
                                """)
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
}