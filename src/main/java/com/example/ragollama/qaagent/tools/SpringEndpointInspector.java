package com.example.ragollama.qaagent.tools;

import com.example.ragollama.qaagent.model.EndpointInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.stream.Stream;

/**
 * Инфраструктурный сервис для интроспекции рантайм-контекста Spring MVC.
 * <p>
 * Предоставляет надежный способ получить список всех HTTP-эндпоинтов,
 * фактически зарегистрированных и работающих в приложении.
 */
@Slf4j
@Service
public class SpringEndpointInspector {

    private final RequestMappingHandlerMapping handlerMapping;

    /**
     * Конструктор для внедрения зависимости {@link RequestMappingHandlerMapping}.
     * <p>
     * Используется {@code @Qualifier("requestMappingHandlerMapping")}, чтобы явно
     * выбрать бин, отвечающий за маппинг бизнес-контроллеров, и избежать
     * конфликта с бином от Spring Boot Actuator.
     *
     * @param handlerMapping Основной обработчик маппингов запросов Spring MVC.
     */
    public SpringEndpointInspector(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    /**
     * Получает список всех реализованных в приложении эндпоинтов.
     * <p>
     * Метод сканирует зарегистрированные обработчики и фильтрует их,
     * исключая системные эндпоинты, такие как Actuator и страницы ошибок,
     * для предоставления чистого списка бизнес-эндпоинтов.
     *
     * @return Список объектов {@link EndpointInfo}, представляющих бизнес-эндпоинты.
     */
    public List<EndpointInfo> getImplementedEndpoints() {
        // Исключаем системные эндпоинты Spring Boot (actuator, error)
        List<String> excludedPrefixes = List.of("/error", "/actuator");

        return handlerMapping.getHandlerMethods().entrySet().stream()
                .flatMap(entry -> {
                    RequestMappingInfo mappingInfo = entry.getKey();
                    return createEndpointInfoStream(mappingInfo);
                })
                .filter(endpoint -> excludedPrefixes.stream().noneMatch(prefix -> endpoint.path().startsWith(prefix)))
                .distinct()
                .toList();
    }

    /**
     * Вспомогательный метод для преобразования {@link RequestMappingInfo} в поток {@link EndpointInfo}.
     * <p>
     * Один {@code @RequestMapping} может определять несколько путей или методов,
     * поэтому этот метод корректно обрабатывает все комбинации.
     *
     * @param mappingInfo Информация о маппинге из Spring.
     * @return Поток унифицированных объектов {@link EndpointInfo}.
     */
    private Stream<EndpointInfo> createEndpointInfoStream(RequestMappingInfo mappingInfo) {
        // Один @RequestMapping может определять несколько путей или методов
        return mappingInfo.getPatternsCondition().getPatterns().stream()
                .flatMap(path -> mappingInfo.getMethodsCondition().getMethods().stream()
                        .map(method -> new EndpointInfo(path, HttpMethod.valueOf(method.name())))
                );
    }
}
