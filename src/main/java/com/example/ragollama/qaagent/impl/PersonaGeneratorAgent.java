package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.model.AttackPersonas;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-агент, который синтезирует "персоны" для тестирования
 * на основе извлеченных правил контроля доступа.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersonaGeneratorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "persona-generator";
    }

    @Override
    public String getDescription() {
        return "Генерирует набор атакующих и легитимных персон на основе правил RBAC.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("extractedRules");
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        List<Map<String, String>> rbacRules = (List<Map<String, String>>) context.payload().get("extractedRules");
        if (rbacRules.isEmpty()) {
            return CompletableFuture.completedFuture(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Правила RBAC не найдены, генерация персон пропущена.", Map.of()));
        }

        try {
            String rbacJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rbacRules);
            String promptString = promptService.render("personaGenerator", Map.of("rbac_rules_json", rbacJson));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApply(this::parseLlmResponse)
                    .thenApply(attackPersonas -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Персоны для атаки успешно сгенерированы.",
                            Map.of("attackPersonas", attackPersonas)
                    ));
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации правил RBAC", e));
        }
    }

    private AttackPersonas parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, AttackPersonas.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от Persona Generator LLM: {}", jsonResponse, e);
            throw new ProcessingException("Persona Generator LLM вернул невалидный JSON.", e);
        }
    }
}
