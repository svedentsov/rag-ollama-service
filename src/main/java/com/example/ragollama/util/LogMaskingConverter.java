package com.example.ragollama.util;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Кастомный конвертер для Logback, который маскирует конфиденциальные данные в логах.
 * <p>
 * Этот конвертер находит в лог-сообщениях паттерны, соответствующие
 * конфиденциальным данным (например, поля "password"), и заменяет их значения
 * на маску (например, "********"). Используется в {@code logback-spring.xml}.
 */
public class LogMaskingConverter extends ClassicConverter {

    // Паттерн для поиска полей типа "password": "someValue"
    private static final Pattern SENSITIVE_DATA_PATTERN = Pattern.compile(
            "(\"password\"\\s*:\\s*\")([^\"]*)(\")",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String convert(ILoggingEvent event) {
        // Получаем отформатированное сообщение и применяем маскирование
        return mask(event.getFormattedMessage());
    }

    /**
     * Применяет маскирование к строке.
     *
     * @param message Исходное сообщение для маскирования.
     * @return Сообщение с замаскированными данными.
     */
    private String mask(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }
        // Используем Matcher для поиска и замены всех вхождений
        Matcher matcher = SENSITIVE_DATA_PATTERN.matcher(message);
        return matcher.replaceAll("$1********$3");
    }
}
