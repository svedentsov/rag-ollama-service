package com.example.ragollama.shared.prompts;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Утилитарный класс, реализующий механизм временного переопределения промптов
 * для текущего потока с использованием {@link ThreadLocal}.
 * <p>
 * Это ключевой компонент для безопасного A/B-тестирования промптов в
 * многопоточной среде.
 */
@Slf4j
@UtilityClass
public class ThreadLocalPromptOverride {

    private static final ThreadLocal<Map<String, String>> OVERRIDES = ThreadLocal.withInitial(ConcurrentHashMap::new);

    /**
     * Устанавливает временное содержимое для указанного шаблона промпта.
     *
     * @param promptName Имя промпта (например, "ragPrompt").
     * @param content    Новое содержимое.
     */
    public void setOverride(String promptName, String content) {
        OVERRIDES.get().put(promptName, content);
        log.warn("Установлено временное переопределение для промпта '{}' в текущем потоке.", promptName);
    }

    /**
     * Получает временное содержимое промпта, если оно было установлено.
     *
     * @param promptName Имя промпта.
     * @return Содержимое или {@code null}, если переопределения нет.
     */
    public String getOverride(String promptName) {
        return OVERRIDES.get().get(promptName);
    }

    /**
     * Полностью очищает все переопределения для текущего потока.
     * Должен вызываться в блоке `finally` для предотвращения утечек памяти.
     */
    public void clear() {
        if (!OVERRIDES.get().isEmpty()) {
            log.warn("Очистка временных переопределений промптов для текущего потока.");
            OVERRIDES.remove();
        }
    }
}