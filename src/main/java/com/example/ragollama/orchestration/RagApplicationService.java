package com.example.ragollama.orchestration;

import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.rag.api.dto.RagQueryRequest;
import com.example.ragollama.rag.api.dto.RagQueryResponse;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.pipeline.RagPipelineOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagApplicationService {

    private final RagPipelineOrchestrator ragPipelineOrchestrator;
    private final DialogManager dialogManager;

    public CompletableFuture<RagQueryResponse> processRagRequestAsync(RagQueryRequest request) {
        return dialogManager.startTurn(request.sessionId(), request.query(), MessageRole.USER)
                .thenCompose(turnContext ->
                        ragPipelineOrchestrator.queryAsync(
                                        request.query(),
                                        turnContext.history(),
                                        request.topK(),
                                        request.similarityThreshold(),
                                        turnContext.sessionId()
                                )
                                .thenCompose(ragAnswer ->
                                        dialogManager.endTurn(turnContext.sessionId(), ragAnswer.answer(), MessageRole.ASSISTANT)
                                                // ИСПРАВЛЕНИЕ: Передаем все поля из ragAnswer в конструктор RagQueryResponse
                                                .thenApply(v -> new RagQueryResponse(
                                                        ragAnswer.answer(),
                                                        ragAnswer.sourceCitations(),
                                                        turnContext.sessionId(),
                                                        ragAnswer.trustScoreReport()
                                                ))
                                )
                );
    }

    public Flux<StreamingResponsePart> processRagRequestStream(RagQueryRequest request) {
        return Mono.fromFuture(() -> processRagRequestAsync(request))
                .flatMapMany(response -> Flux.concat(
                        Flux.just(new StreamingResponsePart.Content(response.answer())),
                        Flux.just(new StreamingResponsePart.Sources(response.sourceCitations())),
                        Flux.just(new StreamingResponsePart.Done("Успешно завершено"))
                ));
    }
}
