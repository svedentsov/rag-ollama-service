package com.example.ragollama.agent.testanalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.testanalysis.model.MentorshipReport;
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

/**
 * AI-агент, выступающий в роли "Наставника по Тестированию".
 * <p>
 * Проводит глубокое, многоаспектное ревью кода автотеста, сопоставляя его
 * с исходными требованиями и лучшими практиками разработки.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestMentorAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "test-mentor-bot";
    }

    @Override
    public String getDescription() {
        return "Выполняет роль AI-наставника, предоставляя детальное код-ревью для автотеста.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("requirementsText") && context.payload().containsKey("testCode");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String requirements = (String) context.payload().get("requirementsText");
        String testCode = (String) context.payload().get("testCode");

        String promptString = promptService.render("testMentorBotPrompt", Map.of(
                "requirements", requirements,
                "test_code", testCode
        ));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                .map(tuple -> parseLlmResponse(tuple.getT1()))
                .map(report -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        report.overallFeedback(),
                        Map.of("mentorshipReport", report)
                ));
    }

    private MentorshipReport parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, MentorshipReport.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от Test Mentor LLM: {}", jsonResponse, e);
            throw new ProcessingException("Test Mentor LLM вернул невалидный JSON.", e);
        }
    }
}
