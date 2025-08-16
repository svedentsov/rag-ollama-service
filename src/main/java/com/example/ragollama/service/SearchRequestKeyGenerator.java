package com.example.ragollama.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * Кастомный генератор ключей для кэширования результатов векторного поиска.
 * Создает надежный ключ на основе содержимого {@link SearchRequest}.
 * Ключ нормализуется и хешируется для повышения эффективности кэша.
 * Теперь это простой компонент, а не реализация KeyGenerator.
 */
@Component
@Slf4j
public class SearchRequestKeyGenerator {

    private static final String PUNCTUATION_REGEX = "[\\p{Punct}\\s]+";

    /**
     * Генерирует ключ кэша для заданного {@link SearchRequest}.
     *
     * @param request Объект запроса, для которого нужно сгенерировать ключ.
     * @return Сгенерированный и хешированный ключ в виде строки.
     */
    public String generate(SearchRequest request) {
        if (request == null) {
            log.warn("Попытка сгенерировать ключ для null-запроса. Возвращен пустой ключ.");
            return "";
        }

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
}
