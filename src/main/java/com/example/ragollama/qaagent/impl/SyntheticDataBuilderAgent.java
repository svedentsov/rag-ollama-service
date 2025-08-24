package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.QaAgent;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент для генерации синтетических (моковых) данных на основе
 * определения Java-класса.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyntheticDataBuilderAgent implements QaAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "synthetic-data-builder";
    }

    @Override
    public String getDescription() {
        return "Генерирует реалистичные моковые данные в формате JSON на основе определения Java-класса.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("classDefinition") && context.payload().containsKey("count");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String classDefinition = (String) context.payload().get("classDefinition");
        Integer count = (Integer) context.payload().get("count");

        log.info("SyntheticDataBuilderAgent: запуск генерации {} мок-объектов.", count);

        String promptString = promptService.render("syntheticDataBuilder", Map.of(
                "classDefinition", classDefinition,
                "count", count
        ));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .thenApply(this::parseLlmResponse)
                .thenApply(mockData -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        String.format("Успешно сгенерировано %d мок-объектов.", mockData.size()),
                        Map.of("mockData", mockData)
                ));
    }

    private List<Map<String, Object>> parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonResponse.replaceAll("(?s)```json\\s*|\\s*```", "").trim();
            if (cleanedJson.isEmpty() || cleanedJson.equals("[]")) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM для SyntheticData: {}", jsonResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON для SyntheticData.", e);
        }
    }
}
