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
     * <p>
     * Ищет ключи (в кавычках или без), такие как "password", "token", "secret", "apiKey",
     * за которыми следует разделитель (двоеточие или знак равенства) и значение в кавычках.
     * Захватывает в группы префикс (группа 1) и суффикс (группа 3) для корректной замены.
     */
    private static final Pattern SENSITIVE_DATA_PATTERN = Pattern.compile(
            "([\"']?(?:password|token|secret|apiKey)[\"']?\\s*[:=]\\s*[\"'])([^\"']*)([\"'])",
            Pattern.CASE_INSENSITIVE);

    /**
     * Преобразует событие логгирования, маскируя его отформатированное сообщение.
     * <p>
     * Этот метод является точкой входа для Logback. Он получает событие,
     * извлекает из него уже отформатированное сообщение и передает его в нашу логику маскирования.
     *
     * @param event Событие логгирования, содержащее всю информацию о лог-записи.
     * @return Отформатированное и замаскированное сообщение для вывода в лог.
     */
    @Override
    public String convert(ILoggingEvent event) {
        return mask(event.getFormattedMessage());
    }

    /**
     * Применяет маскирование ко всем найденным в строке конфиденциальным данным.
     *
     * @param message Исходное сообщение для маскирования.
     * @return Сообщение с замаскированными данными, либо исходное сообщение,
     * если оно пустое или не содержит чувствительных данных.
     */
    private String mask(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }
        // Используем Matcher для поиска и замены всех вхождений
        Matcher matcher = SENSITIVE_DATA_PATTERN.matcher(message);
        // replaceAll заменяет найденное значение на комбинацию захваченных групп
        // $1 - префикс, $3 - суффикс. Между ними вставляется маска.
        return matcher.replaceAll("$1********$3");
    }
}
