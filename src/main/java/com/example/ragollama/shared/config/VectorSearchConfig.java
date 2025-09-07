package com.example.ragollama.shared.config;

import com.example.ragollama.rag.retrieval.search.DefaultVectorSearchService;
import com.example.ragollama.rag.retrieval.search.VectorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Конфигурация для сборки и внедрения сервиса векторного поиска.
 * <p> После удаления Redis, эта конфигурация была значительно упрощена.
 * Она теперь напрямую предоставляет {@link DefaultVectorSearchService} как
 * основную и единственную реализацию {@link VectorSearchService}.
 * Декоратор для кэширования больше не требуется.
 */
@Configuration
@RequiredArgsConstructor
public class VectorSearchConfig {

    private final DefaultVectorSearchService defaultVectorSearchService;

    /**
     * Предоставляет основной бин {@link VectorSearchService}.
     *
     * <p>Аннотация {@code @Primary} гарантирует, что именно этот экземпляр
     * будет внедряться по умолчанию во все компоненты, которые запрашивают
     * интерфейс {@link VectorSearchService}.
     *
     * @return Финальный, готовый к использованию в приложении экземпляр сервиса.
     */
    @Bean
    @Primary
    public VectorSearchService vectorSearchService() {
        return defaultVectorSearchService;
    }
}
