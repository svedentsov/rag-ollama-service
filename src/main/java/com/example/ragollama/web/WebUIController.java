package com.example.ragollama.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.UUID;

/**
 * Контроллер для отображения веб-интерфейса чата.
 * ИСПРАВЛЕНО: Добавлена логика для передачи режима разработки в шаблон.
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
     * Отображает главную страницу.
     * @return ModelAndView для шаблона 'index'.
     */
    @GetMapping("/")
    public ModelAndView getIndexPage() {
        ModelAndView mav = new ModelAndView("index");
        mav.addObject("isDevelopmentMode", isDevelopmentMode());
        return mav;
    }

    /**
     * Отображает страницу чата, опционально для существующей сессии.
     *
     * @param sessionIdAsString Опциональный ID сессии чата в виде строки.
     * @return ModelAndView для шаблона 'chat'.
     */
    @GetMapping("/chat")
    public ModelAndView getChatPage(@RequestParam(required = false) String sessionIdAsString) {
        ModelAndView mav = new ModelAndView("chat");
        mav.addObject("isDevelopmentMode", isDevelopmentMode());

        if (sessionIdAsString != null && !sessionIdAsString.isBlank()) {
            try {
                UUID sessionId = UUID.fromString(sessionIdAsString);
                mav.addObject("sessionId", sessionId.toString());
            } catch (IllegalArgumentException e) {
                mav.addObject("error", "Invalid session ID format.");
            }
        }
        return mav;
    }
}
