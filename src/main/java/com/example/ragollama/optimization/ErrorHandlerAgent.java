package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.optimization.model.RemediationPlan;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorHandlerAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "error-handler-agent";
    }

    @Override
    public String getDescription() {
        return "Диагностирует ошибку в работе другого агента и генерирует план по ее исправлению.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("failedAgentName") && context.payload().containsKey("errorMessage");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String promptString = promptService.render("errorHandlerPrompt", context.payload());

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                .map(tuple -> parseLlmResponse(tuple.getT1()))
                .map(plan -> {
                    log.info("ErrorHandlerAgent сгенерировал план исправления: {}", plan.action());
                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "План исправления сгенерирован: " + plan.action(),
                            Map.of("remediationPlan", plan)
                    );
                });
    }

    private RemediationPlan parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, RemediationPlan.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("ErrorHandlerAgent LLM вернул невалидный JSON.", e);
        }
    }
}
