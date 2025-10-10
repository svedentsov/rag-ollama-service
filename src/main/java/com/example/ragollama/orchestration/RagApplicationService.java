package com.example.ragollama.orchestration;

import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.rag.api.dto.RagQueryRequest;
import com.example.ragollama.rag.api.dto.RagQueryResponse;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.pipeline.steps.RagPipelineOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Сервис-фасад для RAG, адаптированный для реактивного стека.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagApplicationService {

    private final RagPipelineOrchestrator ragPipelineOrchestrator;
    private final DialogManager dialogManager;

    /**
     * Асинхронно обрабатывает RAG-запрос.
     *
     * @param request DTO с запросом.
     * @param taskId  ID задачи.
     * @return {@link Mono} с полным ответом.
     */
    public Mono<RagQueryResponse> processRagRequestAsync(RagQueryRequest request, UUID taskId) {
        return dialogManager.startTurn(request.sessionId(), request.query(), MessageRole.USER)
                .flatMap(turnContext ->
                        ragPipelineOrchestrator.queryAsync(
                                        request.query(),
                                        turnContext.history(),
                                        request.topK(),
                                        request.similarityThreshold(),
                                        turnContext.sessionId()
                                )
                                .flatMap(ragAnswer ->
                                        dialogManager.endTurn(turnContext.sessionId(), turnContext.userMessageId(), ragAnswer.answer(), MessageRole.ASSISTANT, taskId)
                                                .thenReturn(new RagQueryResponse(
                                                        ragAnswer.answer(),
                                                        ragAnswer.sourceCitations(),
                                                        turnContext.sessionId(),
                                                        ragAnswer.trustScoreReport(),
                                                        ragAnswer.validationReport()
                                                ))
                                )
                );
    }

    /**
     * Обрабатывает RAG-запрос в потоковом режиме.
     *
     * @param request DTO с запросом.
     * @param taskId  ID задачи.
     * @return {@link Flux} с частями ответа.
     */
    public Flux<StreamingResponsePart> processRagRequestStream(RagQueryRequest request, UUID taskId) {
        return dialogManager.startTurn(request.sessionId(), request.query(), MessageRole.USER)
                .flatMapMany(turnContext -> {
                    final StringBuilder fullResponseBuilder = new StringBuilder();
                    return ragPipelineOrchestrator.queryStream(
                                    request.query(),
                                    turnContext.history(),
                                    request.topK(),
                                    request.similarityThreshold(),
                                    turnContext.sessionId()
                            )
                            .doOnNext(part -> {
                                if (part instanceof StreamingResponsePart.Content content) {
                                    fullResponseBuilder.append(content.text());
                                }
                            })
                            .doOnComplete(() -> {
                                String fullResponse = fullResponseBuilder.toString();
                                if (!fullResponse.isBlank()) {
                                    dialogManager.endTurn(turnContext.sessionId(), turnContext.userMessageId(), fullResponse, MessageRole.ASSISTANT, taskId).subscribe();
                                }
                            });
                });
    }
}
