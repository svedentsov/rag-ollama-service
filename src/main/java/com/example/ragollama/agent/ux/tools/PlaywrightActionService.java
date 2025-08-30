package com.example.ragollama.agent.ux.tools;

import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис-инструмент, который предоставляет "руки и глаза" для AI-агента.
 * <p>
 * Инкапсулирует всю сложность взаимодействия с Playwright, предоставляя
 * AI-агенту простой и атомарный набор команд для управления браузером.
 * <p>
 * ВАЖНО: Этот компонент является stateful внутри одного вызова агента.
 */
@Slf4j
@Service
public class PlaywrightActionService {
    // В реальной системе это должно быть в ThreadLocal или управляться жизненным циклом сессии
    private Browser browser;
    private Page page;

    /**
     * Инициализирует сессию Playwright.
     */
    public void startSession() {
        if (browser == null || !browser.isConnected()) {
            Playwright playwright = Playwright.create();
            browser = playwright.chromium().launch();
        }
        page = browser.newPage();
    }

    /**
     * Завершает сессию Playwright.
     */
    public void closeSession() {
        if (page != null) page.close();
        // В реальном приложении браузер может оставаться открытым для переиспользования
    }

    public void goTo(String url) {
        page.navigate(url);
    }

    public void click(String selector) {
        page.click(selector);
    }

    public void fill(String selector, String text) {
        page.fill(selector, text);
    }

    public void assertText(String selector, String text) {
        String content = page.textContent(selector);
        if (!content.contains(text)) {
            throw new AssertionError(String.format("Assertion failed: Element '%s' does not contain text '%s'. Actual: '%s'", selector, text, content));
        }
    }

    /**
     * Получает "упрощенный" DOM текущей страницы, чтобы передать его LLM.
     *
     * @return Строка с HTML-содержимым.
     */
    public String getDom() {
        // В реальной системе здесь может быть логика для "очистки" DOM,
        // удаления скриптов, стилей и т.д., чтобы уменьшить количество токенов.
        return page.content();
    }
}
