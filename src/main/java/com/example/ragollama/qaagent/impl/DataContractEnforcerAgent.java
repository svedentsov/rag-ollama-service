package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.model.ContractValidationResult;
import com.example.ragollama.qaagent.tools.GitApiClient;
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
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, который проверяет изменения в DTO на обратную совместимость.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataContractEnforcerAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "data-contract-enforcer";
    }

    @Override
    public String getDescription() {
        return "Сравнивает две версии DTO и выявляет ломающие изменения в контракте.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("oldRef") &&
                context.payload().containsKey("newRef") &&
                context.payload().containsKey("filePath");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String oldRef = (String) context.payload().get("oldRef");
        String newRef = (String) context.payload().get("newRef");
        String filePath = (String) context.payload().get("filePath");

        // Шаг 1: Асинхронно получаем обе версии файла
        Mono<String> oldContentMono = gitApiClient.getFileContent(filePath, oldRef).defaultIfEmpty("");
        Mono<String> newContentMono = gitApiClient.getFileContent(filePath, newRef).defaultIfEmpty("");

        return Mono.zip(oldContentMono, newContentMono)
                .flatMap(contents -> {
                    // Шаг 2: Передаем обе версии в LLM для сравнения
                    String promptString = promptService.render("dataContractEnforcer", Map.of(
                            "old_dto_code", contents.getT1(),
                            "new_dto_code", contents.getT2()
                    ));
                    return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED));
                })
                .map(this::parseLlmResponse)
                .map(result -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Проверка контракта завершена. Статус: " + result.validationStatus(),
                        Map.of("contractValidationResult", result)
                ))
                .toFuture();
    }

    private ContractValidationResult parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ContractValidationResult.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от Data Contract LLM: {}", jsonResponse, e);
            throw new ProcessingException("Data Contract LLM вернул невалидный JSON.", e);
        }
    }
}
