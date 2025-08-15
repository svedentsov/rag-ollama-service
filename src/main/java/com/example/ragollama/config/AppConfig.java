package com.example.ragollama.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
     * Spring AI для Ollama использует {@code RestTemplate} "под капотом".
     * Вместо устаревших методов setConnectTimeout/setReadTimeout на builder'е,
     * мы настраиваем таймауты напрямую через ClientHttpRequestFactory, что является
     * современным подходом.
     *
     * @param builder Стандартный {@link RestTemplateBuilder}, предоставляемый Spring Boot.
     * @return Сконфигурированный экземпляр {@link RestTemplate}.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // Применяем кастомную фабрику с настроенными таймаутами
                .requestFactory(this::clientHttpRequestFactory)
                .build();
    }

    /**
     * Создает и настраивает фабрику для HTTP-запросов с таймаутами.
     * @return Сконфигурированный ClientHttpRequestFactory.
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10)); // Таймаут на установку соединения
        factory.setReadTimeout(Duration.ofMinutes(2));   // Таймаут на ожидание ответа
        return factory;
    }
}
