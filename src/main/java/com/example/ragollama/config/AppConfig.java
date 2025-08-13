package com.example.ragollama.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Общая конфигурация приложения.
 * <p>
 * Содержит бины, которые используются в различных частях приложения,
 * например, настроенный HTTP-клиент.
 */
@Configuration
public class AppConfig {

    /**
     * Конфигурирует и создает бин {@link RestTemplate} с заданными таймаутами.
     * <p>
     * Spring AI для Ollama использует {@code RestTemplate} "под капотом" для отправки HTTP-запросов.
     * Некоторые запросы к LLM могут выполняться долго, поэтому важно установить адекватные
     * таймауты на подключение и чтение, чтобы избежать преждевременного разрыва соединения.
     *
     * @param builder Стандартный {@link RestTemplateBuilder}, предоставляемый Spring Boot.
     * @return Сконфигурированный экземпляр {@link RestTemplate}.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10)) // Таймаут на установку соединения
                .setReadTimeout(Duration.ofMinutes(2))      // Таймаут на ожидание ответа (увеличен для LLM)
                .build();
    }
}
