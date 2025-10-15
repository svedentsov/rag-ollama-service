package com.example.ragollama.agent.ux.tools;

import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stateless-сервис, предоставляющий атомарные действия для управления браузером через Playwright.
 * <p>
 * Этот сервис является "руками и глазами" для AI-агента. Он не хранит никакого состояния
 * (экземпляров Page или Browser) и выполняет команды над явно переданным объектом {@link Page}.
 * Такой дизайн обеспечивает полную потокобезопасность и изоляцию сессий.
 */
@Slf4j
@Service
public class PlaywrightActionService {

    /**
     * Выполняет навигацию на указанный URL.
     *
     * @param page Объект страницы Playwright для текущей сессии.
     * @param url  URL для перехода.
     */
    public void goTo(Page page, String url) {
        log.debug("Playwright: навигация на URL '{}'", url);
        page.navigate(url);
    }

    /**
     * Выполняет клик по элементу, найденному по селектору.
     *
     * @param page     Объект страницы Playwright.
     * @param selector CSS-селектор элемента.
     */
    public void click(Page page, String selector) {
        log.debug("Playwright: клик по селектору '{}'", selector);
        page.locator(selector).click();
    }

    /**
     * Заполняет поле ввода текстом.
     *
     * @param page     Объект страницы Playwright.
     * @param selector CSS-селектор поля ввода.
     * @param text     Текст для ввода.
     */
    public void fill(Page page, String selector, String text) {
        log.debug("Playwright: ввод текста в '{}'", selector);
        page.locator(selector).fill(text);
    }

    /**
     * Проверяет, что элемент содержит указанный текст.
     *
     * @param page     Объект страницы Playwright.
     * @param selector CSS-селектор элемента.
     * @param text     Ожидаемый текст.
     * @throws AssertionError если текст не найден.
     */
    public void assertText(Page page, String selector, String text) {
        log.debug("Playwright: проверка текста '{}' в '{}'", text, selector);
        String content = page.locator(selector).textContent();
        if (!content.contains(text)) {
            throw new AssertionError(String.format("Assertion failed: Element '%s' does not contain text '%s'. Actual: '%s'", selector, text, content));
        }
    }

    /**
     * Получает "упрощенный" DOM текущей страницы для передачи в LLM.
     *
     * @param page Объект страницы Playwright.
     * @return Строка с HTML-содержимым видимой части страницы.
     */
    public String getDom(Page page) {
        log.debug("Playwright: получение DOM...");
        return page.locator("body").innerHTML();
    }
}
