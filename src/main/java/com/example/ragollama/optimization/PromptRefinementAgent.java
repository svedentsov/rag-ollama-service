package com.example.ragollama.optimization;

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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptRefinementAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "prompt-refinement-agent";
    }

    @Override
    public String getDescription() {
        return "Анализирует неэффективность и предлагает улучшение для промпта.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("interactionAnalysis");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        Map<String, Object> analysis = (Map<String, Object>) context.payload().get("interactionAnalysis");
        String targetPromptName = "planningAgent";
        String originalPrompt;
        try {
            originalPrompt = new String(new ClassPathResource("prompts/planning-agent-prompt.ftl").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new ProcessingException("Не удалось прочитать исходный промпт.", e));
        }

        try {
            String analysisJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(analysis);
            String promptString = promptService.render("promptRefinement", Map.of(
                    "analysis_json", analysisJson,
                    "prompt_name", targetPromptName,
                    "original_prompt", originalPrompt
            ));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(diff -> new AgentResult(
                            getName(), AgentResult.Status.SUCCESS,
                            "Предложено улучшение для промпта '" + targetPromptName + "'.",
                            Map.of("promptDiff", diff)
                    ));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации анализа.", e));
        }
    }
}
