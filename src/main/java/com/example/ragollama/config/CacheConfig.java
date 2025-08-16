package com.example.ragollama.config;

import com.example.ragollama.service.SearchRequestKeyGenerator;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация, связанная с кэшированием в приложении.
 */
@Configuration
public class CacheConfig {

    /**
     * Создает бин {@link KeyGenerator} для кэширования результатов векторного поиска.
     * Этот генератор используется в аннотации {@code @Cacheable} для создания
     * уникального ключа на основе содержимого объекта {@link SearchRequest}.
     * Он делегирует фактическую генерацию ключа нашему кастомному компоненту
     * {@link SearchRequestKeyGenerator}.
     * Бин имеет уникальное имя <b>vectorSearchCacheKeyGenerator</b>, чтобы избежать
     * конфликтов с другими бинами.
     *
     * @param customKeyGenerator наш компонент для генерации ключей, внедряемый Spring.
     * @return бин KeyGenerator, готовый к использованию в {@code @Cacheable}.
     */
    @Bean("vectorSearchCacheKeyGenerator")
    public KeyGenerator vectorSearchCacheKeyGenerator(SearchRequestKeyGenerator customKeyGenerator) {
        return (target, method, params) -> {
            if (params.length > 0 && params[0] instanceof SearchRequest) {
                return customKeyGenerator.generate((SearchRequest) params[0]);
            }
            throw new IllegalArgumentException("KeyGenerator ожидает параметр типа SearchRequest");
        };
    }
}
