package com.example.ragollama.agent.security.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.git.tools.GitApiClient;
import com.example.ragollama.agent.security.model.SecurityFinding;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * QA-агент, выполняющий статическое сканирование безопасности (SAST)
 * измененного кода с помощью LLM. Он ищет уязвимости, соответствующие
 * общепринятым стандартам, таким как OWASP Top 10.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SastAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "sast-agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Выполняет статическое сканирование (SAST) измененного Java-кода на предмет уязвимостей.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("changedFiles") && context.payload().containsKey("newRef");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<AgentResult> execute(AgentContext context) {
        List<String> changedFiles = (List<String>) context.payload().get("changedFiles");
        String newRef = (String) context.payload().get("newRef");

        return Flux.fromIterable(changedFiles)
                .filter(file -> file.endsWith(".java"))
                .flatMap(file -> gitApiClient.getFileContent(file, newRef)
                        .flatMap(content -> scanFile(file, content))
                )
                .collectList()
                .map(allFindings -> {
                    List<SecurityFinding> flattened = allFindings.stream().flatMap(List::stream).toList();
                    String summary = "SAST-сканирование завершено. Найдено уязвимостей: " + flattened.size();
                    log.info(summary);
                    return new AgentResult(getName(), AgentResult.Status.SUCCESS, summary, Map.of("sastFindings", flattened));
                });
    }

    private Mono<List<SecurityFinding>> scanFile(String filePath, String content) {
        if (content == null || content.isBlank()) {
            return Mono.just(Collections.emptyList());
        }
        String promptString = promptService.render("sastAgentPrompt", Map.of("filePath", filePath, "code", content));
        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .map(this::parseLlmResponse);
    }

    private List<SecurityFinding> parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            if (cleanedJson.isEmpty() || "[]".equals(cleanedJson)) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от SAST LLM: {}", jsonResponse, e);
            throw new ProcessingException("SAST LLM вернул невалидный JSON.", e);
        }
    }
}
