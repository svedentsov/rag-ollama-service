package com.example.ragollama.shared.tokenization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/**
 * Кастомный генератор ключей для кэширования результатов векторного поиска.
 * <p>
 * Реализует интерфейс {@link KeyGenerator}, что позволяет Spring использовать
 * его напрямую в аннотации {@code @Cacheable} по имени бина.
 * Создает надежный и уникальный ключ на основе содержимого объекта {@link SearchRequest},
 * который передается в качестве первого аргумента в кэшируемый метод.
 */
@Slf4j
@Component("searchRequestKeyGenerator")
public class SearchRequestKeyGenerator implements KeyGenerator {

    private static final String PUNCTUATION_REGEX = "[\\p{Punct}\\s]+";

    /**
     * Генерирует ключ кэша для вызова метода.
     * <p>
     * Метод строит детерминированную строку из всех значимых полей
     * объекта {@link SearchRequest} (запрос, topK, порог), нормализует ее
     * и хеширует с помощью MD5 для получения компактного и безопасного ключа.
     *
     * @param target вызываемый объект.
     * @param method вызываемый метод.
     * @param params параметры, переданные в метод.
     * @return сгенерированный и хешированный ключ.
     * @throws IllegalArgumentException если первый параметр не является {@link SearchRequest}.
     */
    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params.length > 0 && params[0] instanceof SearchRequest request) {
            // 1. Нормализация строки запроса
            String normalizedQuery = request.getQuery().toLowerCase()
                    .replaceAll(PUNCTUATION_REGEX, "")
                    .trim();
            // 2. Сборка всех параметров в одну строку
            String rawKey = new StringBuilder()
                    .append(normalizedQuery)
                    .append("_")
                    .append(request.getTopK())
                    .append("_")
                    .append(request.getSimilarityThreshold())
                    .toString();
            // 3. Хеширование для получения безопасного и уникального ключа
            String hashedKey = DigestUtils.md5DigestAsHex(rawKey.getBytes(StandardCharsets.UTF_8));
            log.trace("Сгенерирован ключ кэша: '{}' для запроса: '{}'", hashedKey, request.getQuery());
            return hashedKey;
        }
        throw new IllegalArgumentException("KeyGenerator ожидает первый параметр типа SearchRequest");
    }
}
