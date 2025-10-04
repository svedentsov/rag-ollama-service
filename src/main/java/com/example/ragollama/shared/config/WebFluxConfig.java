package com.example.ragollama.shared.config;

import com.example.ragollama.shared.web.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Конфигурация для Spring WebFlux.
 * <p>
 * Заменяет собой `WebMvcConfig`. Регистрирует кастомные компоненты
 * и настраивает глобальную политику CORS для реактивного стека.
 * Регистрация RateLimitInterceptor была удалена, так как
 * {@link RateLimitFilter} как {@code @Component} регистрируется автоматически.
 */
@Configuration
@RequiredArgsConstructor
public class WebFluxConfig implements WebFluxConfigurer {

    /**
     * Создаем глобальную конфигурацию CORS для WebFlux.
     * <p>
     * Этот метод является стандартным способом настройки CORS в WebFlux.
     *
     * @return Конфигуратор WebFlux с настроенными правилами CORS.
     */
    @Bean
    public WebFluxConfigurer corsConfigurer() {
        return new WebFluxConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") // Применяем только к нашему API
                        .allowedOriginPatterns("http://localhost:*", "http://192.168.*:*") // Разрешаем localhost и локальную сеть
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
