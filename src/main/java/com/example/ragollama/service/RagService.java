package com.example.ragollama.service;

import com.example.ragollama.dto.RagQueryRequest;
import com.example.ragollama.dto.RagQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * Высокоуровневый сервис-фасад для обработки RAG-запросов.
 * <p>
 * Отвечает за:
 * 1. Взаимодействие с API-слоем (контроллером).
 * 2. Проверку безопасности входящих запросов (Prompt Injection).
 * 3. Делегирование основной логики выполнения RAG-конвейера
 * специализированному сервису {@link RagOrchestrationService}.
 * <p>
 * Такая структура разделяет обязанности, делая код более чистым и тестируемым.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final RagOrchestrationService ragOrchestrator;
    private final PromptGuardService promptGuardService;

    /**
     * Обрабатывает асинхронный RAG-запрос, возвращая полный ответ.
     *
     * @param request DTO с запросом пользователя.
     * @return CompletableFuture с финальным ответом.
     */
    public CompletableFuture<RagQueryResponse> queryAsync(RagQueryRequest request) {
        log.info("Получен асинхронный RAG-запрос: '{}'", request.query());
        promptGuardService.checkForInjection(request.query());
        return ragOrchestrator.execute(request.query(), request.topK(), request.similarityThreshold());
    }

    /**
     * Обрабатывает RAG-запрос и возвращает ответ в виде потока.
     *
     * @param request DTO с запросом пользователя.
     * @return {@link Flux} с частями сгенерированного ответа.
     */
    public Flux<String> queryStream(RagQueryRequest request) {
        log.info("Получен потоковый RAG-запрос: '{}'", request.query());
        promptGuardService.checkForInjection(request.query());
        return ragOrchestrator.executeStream(request.query(), request.topK(), request.similarityThreshold());
    }
}
