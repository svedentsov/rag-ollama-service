package com.example.ragollama.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;
import java.util.UUID;

/**
 * Контроллер для отображения веб-интерфейса чата.
 * Использует @Controller, а не @RestController, так как возвращает
 * имя шаблона для рендеринга, а не JSON.
 */
@Controller
public class WebUIController {

    @GetMapping("/")
    public ModelAndView getIndexPage() {
        return new ModelAndView("index");
    }

    @GetMapping("/chat")
    public ModelAndView getChatPage(@RequestParam(required = false) UUID sessionId) {
        ModelAndView mav = new ModelAndView("chat");
        if (sessionId != null) {
            mav.addObject("sessionId", sessionId.toString());
        }
        return mav;
    }

    @GetMapping("/login")
    public ModelAndView getLoginPage(@RequestParam Optional<String> error, @RequestParam Optional<String> logout) {
        ModelAndView modelAndView = new ModelAndView("login");
        error.ifPresent(e -> modelAndView.addObject("error", true));
        logout.ifPresent(l -> modelAndView.addObject("logout", true));
        return modelAndView;
    }
}
