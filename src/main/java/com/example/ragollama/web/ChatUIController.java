package com.example.ragollama.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Контроллер для отображения веб-интерфейса чата.
 * Использует @Controller, а не @RestController, так как возвращает
 * имя шаблона для рендеринга, а не JSON.
 */
@Controller
public class ChatUIController {

    /**
     * Обрабатывает GET-запросы на /chat и возвращает страницу чата.
     *
     * @return ModelAndView, указывающий на шаблон 'chat.ftl'.
     */
    @GetMapping("/chat")
    public ModelAndView getChatPage() {
        return new ModelAndView("chat");
    }
}
