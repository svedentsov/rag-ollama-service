package com.example.ragollama.agent.accessibility.tools;

import com.example.ragollama.agent.accessibility.model.AccessibilityViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис-инструмент, инкапсулирующий вызов внешней библиотеки для анализа доступности.
 * <p>
 * ВАЖНО: Эта реализация является **mock-заглушкой** для демонстрации.
 * В реальном проекте здесь будет интеграция с полноценной библиотекой,
 * например, `axe-core-java` с использованием Selenium/WebDriver для рендеринга страницы.
 */
@Slf4j
@Service
public class AccessibilityScannerService {

    /**
     * Сканирует предоставленный HTML-код на предмет нарушений доступности.
     *
     * @param htmlContent HTML-код страницы для анализа.
     * @return Список обнаруженных нарушений.
     */
    public List<AccessibilityViolation> scan(String htmlContent) {
        log.info("Запуск mock-сканера доступности...");
        // В реальном приложении здесь будет сложная логика с WebDriver и Axe-core.
        // Для демонстрации мы возвращаем предопределенный набор нарушений,
        // если в HTML есть тег <img> без атрибута alt.
        if (htmlContent != null && htmlContent.contains("<img src=")) {
            if (!htmlContent.matches(".*<img[^>]+alt=.*")) {
                return List.of(
                        new AccessibilityViolation(
                                "image-alt",
                                "critical",
                                "Images must have alternate text",
                                "https://dequeuniversity.com/rules/axe/4.4/image-alt",
                                List.of("img[src=\"logo.png\"]")
                        ),
                        new AccessibilityViolation(
                                "color-contrast",
                                "serious",
                                "Elements must have sufficient color contrast",
                                "https://dequeuniversity.com/rules/axe/4.4/color-contrast",
                                List.of("button.primary")
                        )
                );
            }
        }
        return List.of();
    }
}
