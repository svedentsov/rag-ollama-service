package com.example.ragollama.shared.config;

import com.example.ragollama.shared.web.RateLimitInterceptor;
import com.example.ragollama.shared.web.RequestIdFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфигурация для Spring Web MVC.
 * Регистрирует кастомные компоненты, такие как фильтры и интерсепторы,
 * для обработки веб-запросов. В этой версии удалена кастомная конфигурация
 * {@code configureAsyncSupport}, чтобы полностью полагаться на автоконфигурацию
 * Spring Boot, которая корректно работает с `micrometer-context-propagation`
 * для асинхронной обработки и трассировки.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * Регистрирует интерсептор для ограничения частоты запросов.
     * Интерсептор будет применяться ко всем эндпоинтам, начинающимся с "/api/",
     * если свойство `app.rate-limiting.enabled` установлено в `true`.
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
     * Фильтр добавляет уникальный ID к каждому входящему запросу, помещая его
     * в MDC (для логов) и в заголовок ответа (`X-Request-ID`).
     * Установка {@code Ordered.HIGHEST_PRECEDENCE} гарантирует, что этот фильтр
     * будет выполнен самым первым в цепочке.
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
}
