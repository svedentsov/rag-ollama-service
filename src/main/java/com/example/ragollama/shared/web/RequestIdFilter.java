package com.example.ragollama.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Сервлет-фильтр для добавления уникального ID к каждому HTTP-запросу.
 * <p>
 * Этот ID используется для сквозной трассировки запросов в логах.
 * Он добавляется в MDC (Mapped Diagnostic Context) Logback и в заголовок ответа.
 * Фильтр гарантирует, что для каждого запроса будет сгенерирован уникальный
 * идентификатор, что критически важно для отладки в распределенных и
 * высоконагруженных системах.
 */
public class RequestIdFilter extends OncePerRequestFilter {
    /**
     * Имя HTTP-заголовка, в котором передается ID запроса.
     */
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_KEY = "requestId";

    /**
     * Основной метод фильтра, выполняемый для каждого запроса.
     *
     * @param request     HTTP-запрос.
     * @param response    HTTP-ответ.
     * @param filterChain Цепочка фильтров.
     * @throws ServletException в случае ошибки сервлета.
     * @throws IOException      в случае ошибки ввода-вывода.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Пытаемся получить ID из заголовка, если он был передан извне (например, от API Gateway)
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isBlank()) {
                // Если ID нет, генерируем новый
                requestId = UUID.randomUUID().toString();
            }
            // Кладем ID в MDC, чтобы он был доступен в паттерне логгирования (%X{requestId})
            MDC.put(MDC_KEY, requestId);
            // Добавляем ID в заголовок ответа, чтобы клиент тоже его видел
            response.setHeader(REQUEST_ID_HEADER, requestId);
            // Передаем управление дальше по цепочке фильтров
            filterChain.doFilter(request, response);
        } finally {
            // Обязательно очищаем MDC после завершения обработки запроса,
            // чтобы избежать утечки ID в логи других потоков
            MDC.remove(MDC_KEY);
        }
    }
}
