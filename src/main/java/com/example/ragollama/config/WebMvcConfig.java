package com.example.ragollama.config;

import com.example.ragollama.web.RateLimitInterceptor;
import com.example.ragollama.web.RequestIdFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфигурация Spring Web MVC для регистрации кастомных интерсепторов и фильтров.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * Регистрирует интерсептор для ограничения частоты запросов.
     * <p>
     * Интерсептор будет применяться ко всем эндпоинтам, начинающимся с {@code /api/**}.
     * Эта регистрация является условной и активируется только если в {@code application.yml}
     * свойство {@code app.rate-limiting.enabled} установлено в {@code true}.
     *
     * @param registry Реестр интерсепторов, предоставляемый Spring MVC.
     */
    @Override
    @ConditionalOnProperty(name = "app.rate-limiting.enabled", havingValue = "true")
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }

    /**
     * Регистрирует сервлет-фильтр {@link RequestIdFilter} для добавления
     * уникального идентификатора к каждому входящему запросу.
     * <p>
     * Это обеспечивает возможность сквозной трассировки запросов в логах
     * с помощью MDC (Mapped Diagnostic Context). Фильтру присваивается
     * наивысший приоритет, чтобы ID запроса был доступен как можно раньше.
     *
     * @return Регистрационный бин для {@link RequestIdFilter}.
     */
    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        FilterRegistrationBean<RequestIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestIdFilter());
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }
}
