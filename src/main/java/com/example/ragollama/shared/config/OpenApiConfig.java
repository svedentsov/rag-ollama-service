package com.example.ragollama.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация для OpenAPI (Swagger) документации.
 * <p>
 * Эта версия использует аннотации для полного определения метаданных API
 * и, что самое важное, для описания схемы безопасности (Basic Authentication).
 * Это позволяет Swagger UI отображать кнопку "Authorize" для интерактивной аутентификации.
 */
@Configuration
@SecurityScheme(
        name = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic")
@OpenAPIDefinition(
        security = @SecurityRequirement(name = "basicAuth"))
public class OpenApiConfig {

    /**
     * Создает и настраивает главный бин {@link OpenAPI}.
     * Здесь задается основная информация о API, которая будет отображаться
     * вверху страницы Swagger UI. Это помогает пользователям API
     * понять его назначение и основные возможности.
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
