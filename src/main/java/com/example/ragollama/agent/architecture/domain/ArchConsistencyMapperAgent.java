package com.example.ragollama.agent.architecture.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.architecture.model.ArchValidationReport;
import com.example.ragollama.agent.git.tools.GitApiClient;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * QA-агент, который проверяет соответствие измененного кода
 * предопределенным архитектурным принципам.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArchConsistencyMapperAgent implements ToolAgent {

    private final GitApiClient gitApiClient;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "arch-consistency-mapper";
    }

    @Override
    public String getDescription() {
        return "Сравнивает измененный код с эталонной архитектурой и находит отклонения.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("architecturePrinciples") &&
                context.payload().containsKey("changedFiles");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<AgentResult> execute(AgentContext context) {
        String principles = (String) context.payload().get("architecturePrinciples");
        List<String> changedFiles = (List<String>) context.payload().get("changedFiles");
        String ref = (String) context.payload().getOrDefault("newRef", "main");

        // Асинхронно получаем контент всех измененных файлов
        return Flux.fromIterable(changedFiles)
                .filter(file -> file.endsWith(".java"))
                .flatMap(file -> gitApiClient.getFileContent(file, ref)
                        .map(content -> Map.entry(file, content))
                )
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(filesContentMap -> {
                    if (filesContentMap.isEmpty()) {
                        return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Не найдено Java-файлов для анализа.", Map.of()));
                    }
                    try {
                        String codeJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(filesContentMap);
                        String promptString = promptService.render("archConsistencyMapperPrompt", Map.of(
                                "architecture_principles", principles,
                                "changed_code_json", codeJson
                        ));
                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                                .map(tuple -> parseLlmResponse(tuple.getT1()))
                                .map(report -> new AgentResult(
                                        getName(),
                                        AgentResult.Status.SUCCESS,
                                        "Проверка архитектуры завершена. Статус: " + report.overallStatus(),
                                        Map.of("archValidationReport", report)
                                ));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new ProcessingException("Ошибка сериализации кода для анализа", e));
                    }
                });
    }

    private ArchValidationReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ArchValidationReport.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от Arch Mapper LLM: {}", jsonResponse, e);
            throw new ProcessingException("Arch Mapper LLM вернул невалидный JSON.", e);
        }
    }
}
