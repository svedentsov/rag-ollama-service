package com.example.ragollama.config;

import com.example.ragollama.web.RateLimitInterceptor;
import com.example.ragollama.web.RequestIdFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.task.AsyncTaskExecutor; // Импортируем правильный тип
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    // Внедряем наш единый пул потоков
    private final AsyncTaskExecutor applicationTaskExecutor;

    @Override
    @ConditionalOnProperty(name = "app.rate-limiting.enabled", havingValue = "true")
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }

    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        FilterRegistrationBean<RequestIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestIdFilter());
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    /**
     * Конфигурирует поддержку асинхронных запросов, указывая Spring MVC
     * использовать наш кастомный, управляемый пул потоков.
     * ВНИМАНИЕ: Это нужно для CompletableFuture в контроллерах, но может
     * мешать нативной обработке Flux. Если SSE снова не заработает,
     * этот метод нужно будет удалить, но тогда асинхронные методы
     * контроллеров могут потерять MDC.
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(applicationTaskExecutor);
    }
}
