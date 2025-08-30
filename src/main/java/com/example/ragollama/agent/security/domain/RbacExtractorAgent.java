package com.example.ragollama.agent.security.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.git.tools.GitApiClient;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент для извлечения правил контроля доступа (RBAC/ACL) из исходного кода.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RbacExtractorAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "rbac-extractor";
    }

    @Override
    public String getDescription() {
        return "Извлекает правила RBAC/ACL из измененных Java-файлов с помощью LLM.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("changedFiles") && context.payload().containsKey("newRef");
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        List<String> changedFiles = (List<String>) context.payload().get("changedFiles");
        String newRef = (String) context.payload().get("newRef");

        log.info("RbacExtractorAgent: анализ {} измененных файлов в ref '{}'", changedFiles.size(), newRef);

        return Flux.fromIterable(changedFiles)
                .filter(filePath -> filePath.endsWith(".java") && filePath.contains("controller"))
                .flatMap(filePath -> gitApiClient.getFileContent(filePath, newRef)
                        .flatMap(content -> extractRulesFromCode(content, filePath))
                        .onErrorResume(e -> {
                            log.error("Ошибка при обработке файла {}: {}", filePath, e.getMessage());
                            return Mono.empty();
                        }))
                .collectList()
                .map(allRules -> {
                    List<Map<String, String>> flattenedRules = allRules.stream()
                            .flatMap(List::stream)
                            .toList();

                    String summary = String.format("Анализ безопасности завершен. Найдено %d правил доступа.", flattenedRules.size());
                    log.info(summary);

                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            summary,
                            Map.of("extractedRules", flattenedRules)
                    );
                })
                .toFuture();
    }

    /**
     * Вызывает LLM для анализа одного файла и извлечения правил.
     *
     * @param code     Содержимое Java-файла.
     * @param filePath Путь к файлу для контекста.
     * @return {@link Mono} со списком извлеченных правил.
     */
    private Mono<List<Map<String, String>>> extractRulesFromCode(String code, String filePath) {
        if (code == null || code.isBlank()) {
            return Mono.just(List.of());
        }

        String promptString = promptService.render("rbacExtractor", Map.of("code", code, "filePath", filePath));
        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED))
                .map(this::parseLlmResponse);
    }

    /**
     * Безопасно парсит JSON-ответ от LLM.
     */
    private List<Map<String, String>> parseLlmResponse(String llmResponse) {
        try {
            String cleanedJson = llmResponse.replaceAll("(?s)```json\\s*|\\s*```", "").trim();
            // Проверяем, не вернула ли модель пустой объект или массив
            if (cleanedJson.equals("{}") || cleanedJson.equals("[]") || !cleanedJson.contains("resource")) {
                return List.of();
            }
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM для RBAC: {}", llmResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON для RBAC.", e);
        }
    }
}
