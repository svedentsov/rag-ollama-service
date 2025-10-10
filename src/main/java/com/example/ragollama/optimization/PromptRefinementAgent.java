package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.optimization.model.InteractionAnalysisReport;
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
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * AI-агент, выступающий в роли "инженера по промптам".
 * <p>
 * Принимает на вход строго типизированный отчет об неэффективности и исходный текст промпта,
 * а затем генерирует улучшенную версию, которая должна решить обнаруженные проблемы.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptRefinementAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "prompt-refinement-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует неэффективность и предлагает улучшение для промпта.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().get("interactionAnalysis") instanceof InteractionAnalysisReport;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        InteractionAnalysisReport analysis = (InteractionAnalysisReport) context.payload().get("interactionAnalysis");
        String targetPromptName = "planningAgent";
        String originalPrompt;
        try {
            originalPrompt = new String(new ClassPathResource("prompts/planning-agent-prompt.ftl").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return Mono.error(new ProcessingException("Не удалось прочитать исходный промпт.", e));
        }

        try {
            String analysisJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(analysis);
            String promptString = promptService.render("promptRefinementPrompt", Map.of(
                    "analysis_json", analysisJson,
                    "prompt_name", targetPromptName,
                    "original_prompt", originalPrompt
            ));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .map(diff -> new AgentResult(
                            getName(), AgentResult.Status.SUCCESS,
                            "Предложено улучшение для промпта '" + targetPromptName + "'.",
                            Map.of("promptDiff", diff)
                    ));
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации анализа.", e));
        }
    }
}
