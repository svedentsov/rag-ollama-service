package com.example.ragollama.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

import java.util.Arrays;

/**
 * Контроллер для отображения и маршрутизации Single Page Application (SPA).
 * <p>
 * Эта версия реализует механизм "SPA Fallback". Она перехватывает все
 * известные клиентские маршруты (такие как /chat, /files) и для любого
 * из них отдает единую точку входа — шаблон index_spa.ftl. Это позволяет
 * клиентскому роутеру (React Router) корректно обрабатывать "глубокие" ссылки
 * и обновление страниц.
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
     * <p>
     * Этот метод обрабатывает все известные клиентские маршруты (`/`, `/chat/**`, `/files/**`),
     * возвращая для них единую HTML-страницу. Реальная маршрутизация
     * (отображение нужного компонента) происходит на клиенте.
     *
     * @param model Объект модели для передачи данных в шаблон FreeMarker.
     * @return {@link Mono}, содержащий имя шаблона для рендеринга ("index_spa").
     */
    @GetMapping({"/", "/chat/**", "/files/**"})
    public Mono<String> serveSpaShell(Model model) {
        model.addAttribute("isDevelopmentMode", isDevelopmentMode());
        return Mono.just("index_spa");
    }
}
