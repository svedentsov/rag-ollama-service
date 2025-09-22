package com.example.ragollama.shared.tokenization;

import com.example.ragollama.shared.util.FilterExpressionKeyHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Кастомный генератор ключей для кэширования результатов векторного поиска.
 * Создает уникальный ключ на основе всех параметров поиска, включая
 * текст запросов, topK, порог, фильтры и динамический efSearch.
 */
@Slf4j
@Component("searchRequestKeyGenerator")
public class SearchRequestKeyGenerator implements KeyGenerator {

    private static final String PUNCTUATION_REGEX = "[\\p{Punct}\\s]+";

    @Override
    @SuppressWarnings("unchecked")
    public Object generate(Object target, Method method, Object... params) {
        if (params.length < 5) {
            throw new IllegalArgumentException("KeyGenerator ожидает как минимум 5 параметров для метода search.");
        }
        // 1. Извлекаем все параметры из вызова метода
        List<String> queries = (List<String>) params[0];
        int topK = (int) params[1];
        double similarityThreshold = (double) params[2];
        Filter.Expression filter = (Filter.Expression) params[3];
        Integer efSearch = (Integer) params[4];
        // 2. Нормализуем и объединяем запросы
        String normalizedQueries = queries.stream()
                .map(q -> q.toLowerCase().replaceAll(PUNCTUATION_REGEX, "").trim())
                .sorted() // Сортируем, чтобы порядок не влиял на ключ
                .collect(Collectors.joining("|"));
        // 3. Собираем все параметры в одну детерминированную строку
        StringJoiner rawKeyBuilder = new StringJoiner("_")
                .add(normalizedQueries)
                .add("k" + topK)
                .add("t" + similarityThreshold)
                .add("f" + FilterExpressionKeyHelper.generateKey(filter))
                .add("ef" + (efSearch != null ? efSearch.toString() : "default"));
        // 4. Хешируем для получения безопасного и уникального ключа
        String hashedKey = DigestUtils.md5DigestAsHex(rawKeyBuilder.toString().getBytes(StandardCharsets.UTF_8));
        log.trace("Сгенерирован ключ кэша: '{}' для запроса: '{}'", hashedKey, queries.get(0));
        return hashedKey;
    }
}
