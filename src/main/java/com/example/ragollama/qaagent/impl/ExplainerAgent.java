package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, который объясняет решения и результаты работы других агентов.
 * <p>
 * Выступает в роли "переводчика" с технического языка на человеческий,
 * делая выводы системы прозрачными и понятными для всех членов команды.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExplainerAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "explainer-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Объясняет предоставленный технический контекст (например, отчет другого агента) в ответ на вопрос пользователя.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("userQuestion") && context.payload().containsKey("technicalContext");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String userQuestion = (String) context.payload().get("userQuestion");
        Object technicalContext = context.payload().get("technicalContext");

        try {
            String contextAsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(technicalContext);
            String promptString = promptService.render("explainerAgent", Map.of(
                    "userQuestion", userQuestion,
                    "technicalContextJson", contextAsJson
            ));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(explanation -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Объяснение успешно сгенерировано.",
                            Map.of("explanation", explanation)
                    ));

        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать технический контекст для объяснения", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
