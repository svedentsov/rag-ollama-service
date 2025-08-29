package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.ToolAgent;
import com.example.ragollama.qaagent.domain.DataProfilerService;
import com.example.ragollama.qaagent.model.GeneratedDataReport;
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
 * AI-агент, который генерирует большие объемы статистически-релевантных,
 * но полностью синтетических данных.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataGeneratorAgent implements ToolAgent {

    private final DataProfilerService profilerService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "data-generator";
    }

    @Override
    public String getDescription() {
        return "Генерирует большие объемы синтетических данных, статистически идентичных исходному набору.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("sourceSqlQuery") && context.payload().containsKey("recordCount");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String sql = (String) context.payload().get("sourceSqlQuery");
        Integer count = (Integer) context.payload().get("recordCount");

        // Шаг 1: Детерминированно строим точный статистический профиль
        return CompletableFuture.supplyAsync(() -> profilerService.profile(sql))
                .thenCompose(sourceProfile -> {
                    // Шаг 2: Передаем профиль в LLM для генерации данных
                    try {
                        String profileJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sourceProfile);
                        String promptString = promptService.render("dataGenerator", Map.of(
                                "data_profile_json", profileJson,
                                "record_count", count
                        ));

                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                                .thenApply(llmResponse -> parseLlmResponse(llmResponse))
                                .thenApply(syntheticData -> new AgentResult(
                                        getName(),
                                        AgentResult.Status.SUCCESS,
                                        "Синтетические данные успешно сгенерированы.",
                                        Map.of("report", new GeneratedDataReport(
                                                syntheticData.size(),
                                                sourceProfile,
                                                syntheticData
                                        ))
                                ));
                    } catch (JsonProcessingException e) {
                        return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации профиля данных", e));
                    }
                });
    }

    private List<Map<String, Object>> parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от Data Generator LLM: {}", jsonResponse, e);
            throw new ProcessingException("Data Generator LLM вернул невалидный JSON.", e);
        }
    }
}
