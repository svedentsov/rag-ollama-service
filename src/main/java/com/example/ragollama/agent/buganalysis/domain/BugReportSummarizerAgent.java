package com.example.ragollama.agent.buganalysis.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.buganalysis.model.BugReportSummary;
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
 * QA-агент, который анализирует неструктурированный текст баг-репорта
 * и преобразует его в четкий, структурированный формат.
 * <p>
 * Этот агент является первым шагом в конвейере анализа багов, подготавливая
 * качественные данные для последующих агентов, таких как детектор дубликатов.
 * <p>
 * В этой версии агент передает в контекст строго типизированный объект
 * {@link BugReportSummary}, а не "сырую" строку.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BugReportSummarizerAgent implements ToolAgent {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "bug-report-summarizer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Анализирует и структурирует 'сырой' текст баг-репорта.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("rawReportText");
    }

    /**
     * Асинхронно выполняет структурирование текста.
     *
     * @param context Контекст, содержащий 'rawReportText'.
     * @return {@link Mono} с результатом, обогащающим контекст
     *         структурированным отчетом для следующего агента.
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String rawReportText = (String) context.payload().get("rawReportText");
        log.info("BugReportSummarizerAgent: запуск анализа для сырого отчета.");

        String promptString = promptService.render("bugReportSummarizerPrompt", Map.of("rawReport", rawReportText));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                .map(tuple -> parseLlmResponse(tuple.getT1()))
                .map(summary -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Баг-репорт успешно проанализирован и структурирован.",
                        Map.of("bugReportSummary", summary) // Передаем строго типизированный объект
                ));
    }

    /**
     * Безопасно парсит JSON-ответ от LLM в {@link BugReportSummary}.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Десериализованный объект {@link BugReportSummary}.
     * @throws ProcessingException если парсинг не удался.
     */
    private BugReportSummary parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, BugReportSummary.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM для BugReportSummary: {}", jsonResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON для BugReportSummary.", e);
        }
    }
}
