package com.example.ragollama.shared.config;

import com.example.ragollama.rag.domain.retrieval.CachingVectorSearchDecorator;
import com.example.ragollama.rag.retrieval.search.DefaultVectorSearchService;
import com.example.ragollama.rag.retrieval.search.VectorSearchService;
import com.example.ragollama.shared.metrics.MetricService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class VectorSearchConfig {

    private final DefaultVectorSearchService defaultVectorSearchService;
    private final MetricService metricService;

    /**
     * Создает и предоставляет основной, кэширующий экземпляр {@link VectorSearchService}.
     * <p>
     * Этот метод получает базовую реализацию (`defaultVectorSearchService`) и
     * оборачивает ее в кэширующий декоратор. Аннотация {@code @Primary} гарантирует,
     * что именно этот, декорированный, бин будет внедряться по умолчанию
     * во все другие компоненты, которые запрашивают {@link VectorSearchService}.
     *
     * @return Финальный, готовый к использованию в приложении экземпляр сервиса.
     */
    @Bean
    @Primary
    public VectorSearchService vectorSearchService() {
        return new CachingVectorSearchDecorator(defaultVectorSearchService, metricService);
    }
}
