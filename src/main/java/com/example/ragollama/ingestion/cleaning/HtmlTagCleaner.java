package com.example.ragollama.ingestion.cleaning;

import org.jsoup.Jsoup;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Стратегия, удаляющая все HTML-теги из текста.
 * Использует библиотеку Jsoup для надежного и безопасного парсинга.
 */
@Component
@Order(10) // Должна выполняться одной из первых
public class HtmlTagCleaner implements TextCleaner {
    @Override
    public String clean(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return Jsoup.parse(text).text();
    }
}
