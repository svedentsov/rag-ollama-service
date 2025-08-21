package com.example.ragollama.orchestration;

import com.example.ragollama.agent.domain.CodeGenerationService;
import com.example.ragollama.agent.domain.RouterAgentService;
import com.example.ragollama.chat.domain.ChatService;
import com.example.ragollama.orchestration.dto.UniversalRequest;
import com.example.ragollama.orchestration.dto.UniversalResponse;
import com.example.ragollama.rag.domain.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Сервис-оркестратор, который является единой точкой входа для всех запросов.
 * Он использует Router Agent для определения намерения пользователя и делегирует
 * выполнение соответствующему специализированному сервису.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestrationService {

    private final RouterAgentService router;
    private final RagService ragService;
    private final ChatService chatService;
    private final CodeGenerationService codeGenerationService;

    /**
     * Обрабатывает универсальный потоковый запрос.
     *
     * @param request DTO с запросом пользователя.
     * @return Поток {@link Flux} с ответами.
     */
    public Flux<UniversalResponse> processStream(UniversalRequest request) {
        return router.route(request.query())
                .flatMapMany(intent -> switch (intent) {
                    case RAG_QUERY -> ragService.queryStream(request.toRagQueryRequest())
                            .map(UniversalResponse::from);
                    case CHITCHAT -> chatService.processChatRequestStream(request.toChatRequest())
                            .map(UniversalResponse::from);
                    // Генерация кода не поддерживает стриминг в текущей реализации,
                    // но можно легко добавить, если CodeGenerationService вернет Flux
                    case CODE_GENERATION, UNKNOWN ->
                            Mono.fromFuture(() -> codeGenerationService.generateCode(request.toCodeGenerationRequest()))
                                    .map(UniversalResponse::from)
                                    .flux();
                });
    }
}
