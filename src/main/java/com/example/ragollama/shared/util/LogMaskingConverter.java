package com.example.ragollama.shared.util;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Кастомный конвертер для Logback, который маскирует конфиденциальные данные в логах.
 * <p>
 * Этот конвертер находит в лог-сообщениях паттерны, соответствующие
 * конфиденциальным данным (например, поля "password", "token"), и заменяет их значения
 * на маску (например, "********"). Используется в {@code logback-spring.xml}
 * для повышения безопасности и предотвращения утечки секретов в логи.
 */
public class LogMaskingConverter extends ClassicConverter {

    /**
     * Паттерн для поиска полей, содержащих чувствительные данные.
     * Ищет ключи (в кавычках или без), такие как "password", "token", "secret", "apiKey",
     * за которыми следует разделитель (двоеточие или знак равенства) и значение в кавычках.
     */
    private static final Pattern SENSITIVE_DATA_PATTERN = Pattern.compile(
            "([\"']?(?:password|token|secret|apiKey)[\"']?\\s*[:=]\\s*[\"'])([^\"']*)([\"'])",
            Pattern.CASE_INSENSITIVE);

    /**
     * Преобразует событие логгирования, маскируя сообщение.
     *
     * @param event Событие логгирования.
     * @return Отформатированное и замаскированное сообщение.
     */
    @Override
    public String convert(ILoggingEvent event) {
        return mask(event.getFormattedMessage());
    }

    /**
     * Применяет маскирование ко всем найденным в строке конфиденциальным данным.
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
