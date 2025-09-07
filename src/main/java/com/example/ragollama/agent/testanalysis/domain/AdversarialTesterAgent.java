package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.testanalysis.model.GeneratedTestCase;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Агент-"Разрушитель" (Adversarial) в паре.
 * Его задача - проанализировать требования и тест, созданный "Строителем",
 * найти упущенные сценарии и сгенерировать для них негативные/граничные тесты.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdversarialTesterAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "adversarial-tester";
    }

    @Override
    public String getDescription() {
        return "Анализирует существующий тест и генерирует негативные и граничные тесты для упущенных сценариев.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("requirementsText") && context.payload().containsKey("initialTestCase");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String requirements = (String) context.payload().get("requirementsText");
        GeneratedTestCase initialTest = (GeneratedTestCase) context.payload().get("initialTestCase");

        try {
            String initialTestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(initialTest);
            String promptString = promptService.render("adversarialTesterPrompt", Map.of(
                    "requirements", requirements,
                    "initial_test_json", initialTestJson
            ));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(adversarialCases -> {
                        log.info("AdversarialTesterAgent сгенерировал {} дополнительных тестов.", adversarialCases.size());
                        return new AgentResult(
                                getName(),
                                AgentResult.Status.SUCCESS,
                                "Анализ завершен, сгенерировано " + adversarialCases.size() + " дополнительных тестов.",
                                Map.of("adversarialTestCases", adversarialCases)
                        );
                    });
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации исходного теста", e));
        }
    }

    private List<GeneratedTestCase> parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ProcessingException("AdversarialTesterAgent LLM вернул невалидный JSON.", e);
        }
    }
}
