package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.tools.OpenApiQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент для семантического анализа OpenAPI спецификаций.
 * <p>
 * Этот агент является фасадом над {@link OpenApiQueryService}. Он извлекает
 * источник спецификации (URL или контент) и вопрос пользователя из
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
        return (context.payload().containsKey("specUrl") || context.payload().containsKey("specContent"))
                && context.payload().containsKey("query");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String specUrl = (String) context.payload().get("specUrl");
        String specContent = (String) context.payload().get("specContent");
        String query = (String) context.payload().get("query");

        log.info("OpenApiAgent: запуск анализа для запроса '{}'", query);

        CompletableFuture<String> answerFuture = (specUrl != null)
                ? openApiQueryService.querySpecFromUrl(specUrl, query)
                : openApiQueryService.querySpecFromContent(specContent, query);

        return answerFuture.thenApply(answer -> new AgentResult(
                getName(),
                AgentResult.Status.SUCCESS,
                "Анализ спецификации успешно завершен.",
                Map.of("answer", answer)
        ));
    }
}