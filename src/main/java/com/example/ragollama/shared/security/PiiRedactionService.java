package com.example.ragollama.shared.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Сервис, отвечающий за обнаружение и маскирование персональных данных (PII)
 * и секретов в тексте.
 * <p>
 * Сервис использует набор настраиваемых регулярных выражений, которые
 * загружаются из {@code application.yml}. Это позволяет гибко управлять
 * правилами безопасности без изменения кода.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PiiRedactionService {

    private final PiiRedactionProperties properties;
    private List<Pattern> compiledPatterns;

    /**
     * Внутренний класс для хранения конфигурации из {@code application.yml}.
     */
    @Getter
    @Setter
    @ConfigurationProperties(prefix = "app.security.pii-redaction")
    public static class PiiRedactionProperties {
        private boolean enabled = false;
        private List<String> patterns = Collections.emptyList();
    }

    /**
     * Компилирует регулярные выражения один раз при старте для повышения производительности.
     * Этот метод будет вызван автоматически после того, как Spring внедрит {@link PiiRedactionProperties}.
     */
    @jakarta.annotation.PostConstruct
    private void init() {
        if (properties.isEnabled()) {
            this.compiledPatterns = properties.getPatterns().stream()
                    .map(Pattern::compile)
                    .collect(Collectors.toList());
            log.info("PII Redaction Service активирован. Загружено {} паттернов.", compiledPatterns.size());
        } else {
            this.compiledPatterns = Collections.emptyList();
            log.warn("PII Redaction Service выключен. Чувствительные данные НЕ будут маскироваться.");
        }
    }

    /**
     * Применяет все настроенные правила маскирования к входному тексту.
     *
     * @param rawText Исходный текст, который может содержать чувствительные данные.
     * @return Текст с замаскированными данными или исходный текст, если сервис отключен.
     */
    public String redact(String rawText) {
        if (!properties.isEnabled() || rawText == null || rawText.isBlank()) {
            return rawText;
        }

        String redactedText = rawText;
        for (Pattern pattern : compiledPatterns) {
            redactedText = pattern.matcher(redactedText).replaceAll("[REDACTED]");
        }
        return redactedText;
    }
}
