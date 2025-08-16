package com.example.ragollama.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * Сервис, инкапсулирующий логику управления кэшем для результатов векторного поиска.
 * Предоставляет декларативный способ очистки кэша, следуя принципу
 * единственной ответственности (SRP) и скрывая детали реализации от других сервисов.
 * Использование этого сервиса делает код более чистым и устойчивым к изменениям,
 * так как имя кэша и логика его очистки централизованы в одном месте.
 */
@Service
@Slf4j
public class VectorCacheService {

    /**
     * Полностью очищает кэш, в котором хранятся результаты векторного поиска.
     * Эта операция должна вызываться после любого изменения данных в векторном хранилище
     * (например, после индексации нового документа), чтобы предотвратить
     * предоставление устаревших результатов поиска клиентам API.
     * Аннотация {@code @CacheEvict} декларативно указывает Spring на необходимость
     * инвалидации всех записей в кэше "vector_search_results".
     */
    @CacheEvict(value = "vector_search_results", allEntries = true)
    public void evictAll() {
        log.info("Декларативная очистка кэша 'vector_search_results' инициирована.");
    }
}
