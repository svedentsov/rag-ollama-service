package com.example.ragollama.agent.openapi.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.openapi.api.dto.OpenApiSourceRequest;
import com.example.ragollama.agent.openapi.tools.OpenApiQueryService;
import com.example.ragollama.shared.exception.ProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент для семантического анализа OpenAPI спецификаций.
 *
 * <p>Этот агент является фасадом над {@link OpenApiQueryService}. Он извлекает
 * источник спецификации (полиморфный DTO) и вопрос пользователя из
 * {@link AgentContext} и делегирует выполнение RAG-конвейера "на лету"
 * специализированному сервису.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiAgent implements ToolAgent {

    private final OpenApiQueryService openApiQueryService;

    @Override
    public String getName() {
        return "openapi-agent";
    }

    @Override
    public String getDescription() {
        return "Отвечает на вопросы по OpenAPI спецификации, используя динамический RAG-конвейер.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("source") && context.payload().containsKey("query");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        Object sourceObject = context.payload().get("source");
        String query = (String) context.payload().get("query");
        if (!(sourceObject instanceof OpenApiSourceRequest source)) {
            throw new ProcessingException("Ошибка контракта: OpenApiAgent ожидал OpenApiSourceRequest, но получил " +
                    (sourceObject == null ? "null" : sourceObject.getClass().getName()));
        }
        log.info("OpenApiAgent: запуск анализа для запроса '{}'", query);
        CompletableFuture<String> answerFuture = openApiQueryService.querySpec(source, query);
        return answerFuture.thenApply(answer -> new AgentResult(
                getName(),
                AgentResult.Status.SUCCESS,
                "Анализ спецификации успешно завершен.",
                Map.of("answer", answer)
        ));
    }
}
