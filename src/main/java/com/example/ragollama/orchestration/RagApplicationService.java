package com.example.ragollama.orchestration;

import com.example.ragollama.chat.domain.ChatHistoryService;
import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.rag.api.dto.RagQueryRequest;
import com.example.ragollama.rag.api.dto.RagQueryResponse;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.pipeline.RagPipelineOrchestrator;
import com.example.ragollama.shared.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис прикладного уровня (Application Service), оркестрирующий бизнес-логику RAG.
 * <p>
 * Этот сервис является фасадом для всей RAG-функциональности, инкапсулируя
 * управление сессиями, историей и вызовами к "чистому" доменному конвейеру
 * через {@link RagPipelineOrchestrator}.
 * Он следует принципам Clean Architecture, отделяя Web-слой от доменной логики.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagApplicationService {

    private final RagPipelineOrchestrator ragPipelineOrchestrator;
    private final ChatHistoryService chatHistoryService;
    private final AppProperties appProperties;

    /**
     * Асинхронно обрабатывает RAG-запрос, возвращая полный ответ.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link CompletableFuture} с финальным {@link RagQueryResponse}.
     */
    public CompletableFuture<RagQueryResponse> processRagRequestAsync(RagQueryRequest request) {
        final UUID sessionId = getOrCreateSessionId(request.sessionId());

        return saveMessageAndGetHistory(sessionId, request.query())
                .thenCompose(history -> ragPipelineOrchestrator.queryAsync(request.query(), history, request.topK(), request.similarityThreshold(), sessionId))
                .thenCompose(ragAnswer -> saveMessageAsync(sessionId, MessageRole.ASSISTANT, ragAnswer.answer())
                        .thenApply(v -> new RagQueryResponse(ragAnswer.answer(), ragAnswer.sourceCitations(), sessionId)));
    }

    /**
     * Обрабатывает RAG-запрос в потоковом режиме.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link Flux} со структурированными частями ответа.
     */
    public Flux<StreamingResponsePart> processRagRequestStream(RagQueryRequest request) {
        final UUID sessionId = getOrCreateSessionId(request.sessionId());
        return Mono.fromFuture(() -> processRagRequestAsync(request))
                .flatMapMany(response -> Flux.concat(
                        Flux.just(new StreamingResponsePart.Content(response.answer())),
                        Flux.just(new StreamingResponsePart.Sources(response.sourceCitations())),
                        Flux.just(new StreamingResponsePart.Done("Успешно завершено"))
                ));
    }

    /**
     * Атомарно сохраняет сообщение пользователя и загружает актуальную историю чата.
     *
     * @param sessionId   ID сессии.
     * @param userMessage Текст сообщения пользователя.
     * @return {@link CompletableFuture} со списком сообщений, включающим текущее.
     */
    private CompletableFuture<List<Message>> saveMessageAndGetHistory(UUID sessionId, String userMessage) {
        final UserMessage currentMessage = new UserMessage(userMessage);

        CompletableFuture<Void> saveFuture = saveMessageAsync(sessionId, MessageRole.USER, userMessage);
        CompletableFuture<List<Message>> historyFuture = getHistoryAsync(sessionId);

        return saveFuture.thenCombine(historyFuture, (v, history) -> {
            List<Message> mutableHistory = new ArrayList<>(history);
            mutableHistory.add(currentMessage);
            return mutableHistory;
        });
    }

    /**
     * Асинхронно сохраняет сообщение в базу данных.
     *
     * @param sessionId ID сессии.
     * @param role      Роль отправителя.
     * @param content   Текст сообщения.
     * @return {@link CompletableFuture}, завершающийся после сохранения.
     */
    private CompletableFuture<Void> saveMessageAsync(UUID sessionId, MessageRole role, String content) {
        return chatHistoryService.saveMessageAsync(sessionId, role, content)
                .exceptionally(ex -> {
                    log.error("Не удалось сохранить сообщение для сессии {}. Роль: {}", sessionId, role, ex);
                    return null; // Игнорируем ошибку, чтобы не прерывать ответ пользователю.
                });
    }

    /**
     * Асинхронно загружает историю сообщений из базы данных.
     *
     * @param sessionId ID сессии.
     * @return {@link CompletableFuture} со списком сообщений.
     */
    private CompletableFuture<List<Message>> getHistoryAsync(UUID sessionId) {
        int maxHistory = appProperties.chat().history().maxMessages();
        return chatHistoryService.getLastNMessagesAsync(sessionId, maxHistory - 1);
    }

    /**
     * Возвращает ID сессии из запроса или генерирует новый, если он не предоставлен.
     *
     * @param sessionId Опциональный ID из DTO.
     * @return Не-null UUID.
     */
    private UUID getOrCreateSessionId(UUID sessionId) {
        return (sessionId != null) ? sessionId : UUID.randomUUID();
    }
}
