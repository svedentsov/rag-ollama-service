package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.testanalysis.model.GeneratedTestCase;
import com.example.ragollama.agent.testanalysis.model.TestOracleReport;
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

import java.util.List;
import java.util.Map;

/**
 * AI-агент, выступающий в роли "Тестового Оракула" с возможностями объяснения (XAI).
 * <p>
 * Анализирует набор сгенерированных тестов и исходные требования, чтобы
 * построить матрицу трассируемости и выявить пробелы в покрытии.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XaiTestOracleAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "xai-test-oracle";
    }

    @Override
    public String getDescription() {
        return "Анализирует сгенерированные тесты, связывает их с требованиями и находит пробелы в покрытии.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("requirementsText") && context.payload().containsKey("generatedTests");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<AgentResult> execute(AgentContext context) {
        String requirements = (String) context.payload().get("requirementsText");
        List<GeneratedTestCase> generatedTests = (List<GeneratedTestCase>) context.payload().get("generatedTests");

        if (generatedTests.isEmpty()) {
            return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Нет тестов для анализа.", Map.of()));
        }

        try {
            String testsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(generatedTests);
            String promptString = promptService.render("xaiTestOraclePrompt", Map.of(
                    "requirements", requirements,
                    "generated_tests_json", testsJson
            ));

            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .map(this::parseLlmResponse)
                    .map(report -> new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            report.overallAssessment(),
                            Map.of("testOracleReport", report)
                    ));
        } catch (JsonProcessingException e) {
            return Mono.error(new ProcessingException("Ошибка сериализации сгенерированных тестов", e));
        }
    }

    private TestOracleReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, TestOracleReport.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от XAI Test Oracle LLM: {}", jsonResponse, e);
            throw new ProcessingException("XAI Test Oracle LLM вернул невалидный JSON.", e);
        }
    }
}
