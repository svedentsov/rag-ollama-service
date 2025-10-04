package com.example.ragollama.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.util.Arrays;

/**
 * Контроллер для отображения веб-интерфейса чата, адаптированный для WebFlux.
 * <p>
 * Эта версия использует идиоматичный для WebFlux подход: принимает объект
 * {@link Model} и возвращает {@code Mono<String>}, где строка — это имя шаблона.
 * Такой подход полностью неблокирующий и соответствует парадигме реактивного программирования.
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
     * @param sessionIdAsString Опциональный ID сессии чата (игнорируется на бэкенде).
     * @param model             Объект модели для передачи данных в шаблон FreeMarker.
     * @return {@link Mono}, содержащий имя шаблона для рендеринга.
     */
    @GetMapping({"/", "/chat"})
    public Mono<String> getIndexPage(@RequestParam(required = false) String sessionIdAsString, Model model) {
        model.addAttribute("isDevelopmentMode", isDevelopmentMode());
        return Mono.just("index_spa");
    }
}
