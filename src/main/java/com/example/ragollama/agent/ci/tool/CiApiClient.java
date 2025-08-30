package com.example.ragollama.agent.ci.tool;

import com.example.ragollama.agent.config.CiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Абстрактный клиент для взаимодействия с API системы непрерывной интеграции (CI/CD).
 * <p>
 * Инкапсулирует логику отправки HTTP-запросов для запуска задач (jobs).
 * Конфигурация (URL, токен) управляется через {@link CiProperties}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CiApiClient {

    private final WebClient.Builder webClientBuilder;
    private final CiProperties ciProperties;

    /**
     * Отправляет запрос на запуск CI-задачи.
     *
     * @param jobName    Имя или идентификатор задачи для запуска.
     * @param parameters Карта с параметрами для задачи.
     * @return {@link Mono} со строковым ответом от CI-системы.
     */
    public Mono<String> triggerJob(String jobName, Map<String, Object> parameters) {
        WebClient client = webClientBuilder
                .baseUrl(ciProperties.baseUrl())
                .defaultHeaders(headers -> headers.setBasicAuth(ciProperties.username(), ciProperties.apiToken()))
                .build();

        // ПРИМЕЧАНИЕ: Логика ниже является примером для Jenkins API.
        // Для GitHub Actions или другой системы она будет отличаться.
        String uri = String.format("/job/%s/buildWithParameters", jobName);

        log.info("Отправка запроса в CI-систему: POST {}", uri);
        return client.post()
                .uri(uri, uriBuilder -> {
                    parameters.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .retrieve()
                .toBodilessEntity()
                .map(response -> "Задача запущена. Статус: " + response.getStatusCode());
    }
}
