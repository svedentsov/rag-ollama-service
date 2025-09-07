package com.example.ragollama.agent.datageneration.impl;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.datageneration.domain.DataProfilerService;
import com.example.ragollama.agent.datageneration.domain.DifferentialPrivacyService;
import com.example.ragollama.agent.datageneration.model.DataProfile;
import com.example.ragollama.agent.datageneration.model.SyntheticDataReport;
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
 * AI-агент, который генерирует синтетические данные с гарантиями дифференциальной приватности.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DifferentiallyPrivateDataAgent implements ToolAgent {

    private final DataProfilerService profilerService;
    private final DifferentialPrivacyService dpService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "dp-synthetic-data-generator";
    }

    @Override
    public String getDescription() {
        return "Создает статистически-релевантные синтетические данные с гарантиями дифференциальной приватности (DP).";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("sourceSqlQuery");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String sql = (String) context.payload().get("sourceSqlQuery");
        Integer count = (Integer) context.payload().get("recordCount");
        Double epsilon = (Double) context.payload().get("epsilon");

        return CompletableFuture.supplyAsync(() -> profilerService.profile(sql))
                .thenApply(sourceProfile -> {
                    DataProfile privateProfile = dpService.privatize(sourceProfile, epsilon);
                    return Map.entry(sourceProfile, privateProfile);
                })
                .thenCompose(profiles -> {
                    try {
                        String privateProfileJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profiles.getValue());
                        String promptString = promptService.render("dpSyntheticDataGeneratorPrompt", Map.of(
                                "private_profile_json", privateProfileJson,
                                "record_count", count
                        ));

                        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                                .thenApply(llmResponse -> parseLlmResponse(llmResponse))
                                .thenApply(syntheticData -> new AgentResult(
                                        getName(),
                                        AgentResult.Status.SUCCESS,
                                        "Синтетические данные (DP) успешно сгенерированы.",
                                        Map.of("report", new SyntheticDataReport(
                                                syntheticData.size(),
                                                profiles.getKey(),
                                                profiles.getValue(),
                                                syntheticData
                                        ))
                                ));
                    } catch (JsonProcessingException e) {
                        return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации приватного профиля", e));
                    }
                });
    }

    private List<Map<String, Object>> parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от DP Data Generator LLM: {}", jsonResponse, e);
            throw new ProcessingException("DP Data Generator LLM вернул невалидный JSON.", e);
        }
    }
}
