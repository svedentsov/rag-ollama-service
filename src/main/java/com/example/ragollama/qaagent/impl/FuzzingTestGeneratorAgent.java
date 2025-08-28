package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.model.AttackPersonas;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, который генерирует код fuzzing-теста на основе правил RBAC
 * и сгенерированных "персон".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FuzzingTestGeneratorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "fuzzing-test-generator";
    }

    @Override
    public String getDescription() {
        return "Генерирует Java/RestAssured код для RBAC Fuzzing теста, симулирующего IDOR атаку.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("extractedRules") && context.payload().containsKey("attackPersonas");
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        List<Map<String, String>> rbacRules = (List<Map<String, String>>) context.payload().get("extractedRules");
        AttackPersonas personas = (AttackPersonas) context.payload().get("attackPersonas");

        try {
            String rbacJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rbacRules);
            String personasJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(personas);
            String promptString = promptService.render("fuzzingTestGenerator", Map.of(
                    "rbac_rules_json", rbacJson,
                    "personas_json", personasJson
            ));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(generatedCode -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Код Fuzzing-теста успешно сгенерирован.",
                            Map.of("generatedFuzzTest", generatedCode)
                    ));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new RuntimeException("Ошибка сериализации данных для Fuzzing-генератора", e));
        }
    }
}
