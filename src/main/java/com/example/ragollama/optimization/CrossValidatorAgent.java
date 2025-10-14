package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.optimization.model.ConsistencyReport;
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

import java.util.List;
import java.util.Map;

/**
 * Мета-агент, который анализирует доказательства, собранные
 * {@link ConsistencyCheckerAgent}, и выносит вердикт о наличии противоречий.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrossValidatorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "cross-validator-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Сравнивает доказательства из разных источников и находит противоречия.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("allEvidence");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String claim = (String) context.payload().get("claim");
        Object evidenceObject = context.payload().get("allEvidence");

        if (!(evidenceObject instanceof Map)) {
            String errorMessage = "Ошибка контракта: CrossValidatorAgent ожидал Map в 'allEvidence', но получил " +
                    (evidenceObject == null ? "null" : evidenceObject.getClass().getName());
            log.error(errorMessage);
            return Mono.error(new ProcessingException(errorMessage));
        }

        @SuppressWarnings("unchecked")
        Map<String, List<String>> allEvidence = (Map<String, List<String>>) evidenceObject;

        try {
            String evidenceJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(allEvidence);
            String promptString = promptService.render("crossValidatorPrompt", Map.of(
                    "claim", claim,
                    "all_evidence_json", evidenceJson
            ));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                    .map(tuple -> parseLlmResponse(tuple.getT1()))
                    .map(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.summary(),
                            Map.of("consistencyReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации доказательств.", e));
        }
    }

    private ConsistencyReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ConsistencyReport.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("CrossValidatorAgent LLM вернул невалидный JSON.", e);
        }
    }
}
