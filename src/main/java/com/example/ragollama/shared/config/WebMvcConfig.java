package com.example.ragollama.shared.config;

import com.example.ragollama.shared.web.RateLimitInterceptor;
import com.example.ragollama.shared.web.RequestIdFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфигурация для Spring Web MVC.
 * *
 * <p>Регистрирует кастомные компоненты, такие как фильтры и интерсепторы,
 * а также настраивает глобальную политику CORS для всего приложения.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * Регистрирует интерсептор для ограничения частоты запросов.
     *
     * @param registry Реестр, в который добавляется интерсептор.
     */
    @Override
    @ConditionalOnProperty(name = "app.rate-limiting.enabled", havingValue = "true")
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }

    /**
     * Регистрирует сервлет-фильтр {@link RequestIdFilter} для трассировки запросов.
     *
     * @return Объект регистрации для фильтра.
     */
    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        FilterRegistrationBean<RequestIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestIdFilter());
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    /**
     * Создаем глобальную конфигурацию CORS.
     * *
     * <p>Этот метод является стандартным способом настройки CORS в Spring MVC
     * при отсутствии Spring Security. Он позволяет запросы с любых портов
     * на localhost, что идеально для локальной разработки.
     *
     * @return Конфигуратор Web MVC с настроенными правилами CORS.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
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
