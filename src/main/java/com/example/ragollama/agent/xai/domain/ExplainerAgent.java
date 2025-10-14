package com.example.ragollama.agent.xai.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * AI-агент, который объясняет решения и результаты работы других агентов.
 * <p>
 * Эта версия упрощена: она больше не зависит от интерфейса `Explainable`,
 * а принимает любой технический контекст, сериализует его в JSON и передает
 * в LLM для генерации человекочитаемого объяснения.
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
    public Mono<AgentResult> execute(AgentContext context) {
        String userQuestion = (String) context.payload().get("userQuestion");
        Object technicalContext = context.payload().get("technicalContext");

        try {
            // Оборачиваем "сырой" контекст в простую структуру для LLM
            Map<String, Object> contextForPrompt = Map.of(
                    "summary", "Это технические данные, которые нужно объяснить.",
                    "data", technicalContext
            );

            String contextAsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextForPrompt);
            String promptString = promptService.render("explainerAgentPrompt", Map.of(
                    "userQuestion", userQuestion,
                    "technicalContextJson", contextAsJson
            ));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .map(tuple -> {
                        String explanation = tuple.getT1();
                        return new AgentResult(
                                getName(),
                                AgentResult.Status.SUCCESS,
                                "Объяснение успешно сгенерировано.",
                                Map.of("explanation", explanation)
                        );
                    });
        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать технический контекст для объяснения", e);
            // Возвращаем ошибку в реактивной цепочке
            return Mono.error(new ProcessingException("Ошибка сериализации контекста для ExplainerAgent", e));
        }
    }
}
