package com.example.ragollama.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;

/**
 * Контроллер для отображения веб-интерфейса чата.
 * <p>
 * Этот контроллер является единой точкой входа для фронтенд-приложения,
 * написанного на React. Он использует FreeMarker для рендеринга
 * базовой HTML-страницы ("скелета"), которая затем "оживляется"
 * с помощью JavaScript.
 * <p>
 * <b>Архитектурное решение:</b> Вместо того, чтобы рендерить разные
 * HTML-страницы, этот контроллер всегда рендерит один и тот же шаблон-обертку,
 * который содержит единую точку монтирования для React (`<div id="app-root">`).
 * React-приложение само отвечает за всю маршрутизацию на стороне клиента
 * на основе URL, что соответствует подходу Single-Page Application (SPA).
 * Это упрощает бэкенд и переносит всю логику отображения на фронтенд.
 * <p>
 * Контроллер также передает в шаблон флаг `isDevelopmentMode`, который
 * позволяет шаблону подключать скрипты либо с Vite Dev Server (для
 * Hot Module Replacement), либо скомпилированные production-ассеты.
 */
@Controller
public class WebUIController {

    /**
     * Инжектируем массив активных профилей Spring.
     */
    @Value("${spring.profiles.active:}")
    private String[] activeProfiles;

    /**
     * Проверяет, запущен ли сервис в режиме разработки.
     *
     * @return true, если активен профиль "dev" или "development".
     */
    private boolean isDevelopmentMode() {
        if (activeProfiles == null) {
            return false;
        }
        return Arrays.stream(activeProfiles)
                .anyMatch(prof -> prof.equalsIgnoreCase("dev") || prof.equalsIgnoreCase("development"));
    }

    /**
     * Отображает главную страницу-обертку для SPA-приложения.
     * Этот метод обрабатывает все основные URL (`/`, `/chat`), так как
     * реальная маршрутизация происходит на клиенте.
     *
     * @param sessionIdAsString Опциональный ID сессии чата (игнорируется на бэкенде,
     *                          обрабатывается React).
     * @return ModelAndView для корневого шаблона `_layout`, который загружает React.
     */
    @GetMapping({"/", "/chat"})
    public ModelAndView getIndexPage(@RequestParam(required = false) String sessionIdAsString) {
        ModelAndView mav = new ModelAndView("index_spa");
        mav.addObject("isDevelopmentMode", isDevelopmentMode());
        return mav;
    }
}
