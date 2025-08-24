package com.example.ragollama.qaagent.impl;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.QaAgent;
import com.example.ragollama.qaagent.model.BugReportSummary;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * QA-агент, который анализирует неструктурированный текст баг-репорта
 * и преобразует его в четкий, структурированный формат.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BugReportSummarizerAgent implements QaAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "bug-report-summarizer";
    }

    @Override
    public String getDescription() {
        return "Анализирует и структурирует 'сырой' текст баг-репорта.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("rawReportText");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        String rawReportText = (String) context.payload().get("rawReportText");
        log.info("BugReportSummarizerAgent: запуск анализа для сырого отчета.");

        String promptString = promptService.render("bugReportSummarizer", Map.of("rawReport", rawReportText));

        return llmClient.callChat(new Prompt(promptString))
                .thenApply(this::parseLlmResponse)
                .thenApply(summary -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Баг-репорт успешно проанализирован и структурирован.",
                        Map.of("summary", summary)
                ));
    }

    private BugReportSummary parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonResponse.replaceAll("(?s)```json\\s*|\\s*```", "").trim();
            return objectMapper.readValue(cleanedJson, BugReportSummary.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM для BugReportSummary: {}", jsonResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON для BugReportSummary.", e);
        }
    }
}