package com.example.ragollama.config;

import com.example.ragollama.service.CachingVectorSearchDecorator;
import com.example.ragollama.service.MetricService;
import com.example.ragollama.service.VectorSearchService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Конфигурация для сборки и внедрения сервиса векторного поиска.
 * <p>
 * Этот класс явно определяет, как компоненты {@link VectorSearchService}
 * должны быть связаны друг с другом, реализуя паттерн "Декоратор".
 */
@Configuration
public class VectorSearchConfig {

    /**
     * Создает и предоставляет основной, кэширующий экземпляр {@link VectorSearchService}.
     * <p>
     * Этот метод получает базовую реализацию (`defaultVectorSearchService`) и
     * оборачивает ее в кэширующий декоратор. Аннотация {@code @Primary} гарантирует,
     * что именно этот, декорированный, бин будет внедряться по умолчанию
     * во все другие компоненты, которые запрашивают {@link VectorSearchService}.
     *
     * @param defaultService базовая, некэширующая реализация сервиса.
     * @param metricService  сервис для сбора метрик.
     * @return Финальный, готовый к использованию в приложении экземпляр сервиса.
     */
    @Bean
    @Primary
    public VectorSearchService vectorSearchService(
            @Qualifier("defaultVectorSearchService") VectorSearchService defaultService,
            MetricService metricService
    ) {
        return new CachingVectorSearchDecorator(defaultService, metricService);
    }
}
