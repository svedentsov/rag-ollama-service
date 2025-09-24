package com.example.ragollama.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

/**
 * Контроллер для отображения веб-интерфейса чата.
 * Использует @Controller, а не @RestController, так как возвращает
 * имя шаблона для рендеринга, а не JSON.
 */
@Controller
public class WebUIController {

    /**
     * Отображает главную страницу.
     *
     * @return ModelAndView для шаблона 'index'.
     */
    @GetMapping("/")
    public ModelAndView getIndexPage() {
        return new ModelAndView("index");
    }

    /**
     * Отображает страницу чата, опционально для существующей сессии.
     *
     * @param sessionId Опциональный ID сессии чата.
     * @return ModelAndView для шаблона 'chat'.
     */
    @GetMapping("/chat")
    public ModelAndView getChatPage(@RequestParam(required = false) UUID sessionId) {
        ModelAndView mav = new ModelAndView("chat");
        if (sessionId != null) {
            mav.addObject("sessionId", sessionId.toString());
        }
        return mav;
    }
}
