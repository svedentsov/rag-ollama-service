package com.example.ragollama.agent.accessibility.domain;

import com.example.ragollama.agent.accessibility.model.AccessibilityReport;
import com.example.ragollama.agent.accessibility.model.AccessibilityViolation;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Специализированный сервис, инкапсулирующий логику анализа нарушений
 * доступности (a11y) с помощью языковой модели (LLM).
 * <p>
 * Эта версия использует полностью декларативный, идиоматичный реактивный стиль,
 * инкапсулируя потенциально блокирующие операции в {@link Mono#fromCallable}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LlmAccessibilityAnalyzer {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final AccessibilityReportParser reportParser;

    /**
     * Асинхронно анализирует список технических нарушений доступности с помощью LLM.
     *
     * @param violations Список технических нарушений, обнаруженных сканером.
     * @return {@link Mono}, который по завершении будет содержать
     * полностью сформированный {@link AccessibilityReport}.
     * @throws ProcessingException если происходит критическая ошибка при сериализации данных в JSON.
     */
    public Mono<AccessibilityReport> analyze(List<AccessibilityViolation> violations) {
        return Mono.fromCallable(() -> {
                    // Эта операция потенциально блокирующая, поэтому инкапсулируем ее.
                    try {
                        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(violations);
                    } catch (JsonProcessingException e) {
                        log.error("Критическая ошибка: не удалось сериализовать список нарушений a11y в JSON.", e);
                        throw new ProcessingException("Ошибка сериализации нарушений a11y", e);
                    }
                })
                .flatMap(violationsJson -> {
                    String promptString = promptService.render("accessibilityAudit", Map.of("violationsJson", violationsJson));
                    log.debug("Отправка запроса к LLM для анализа {} нарушений доступности.", violations.size());
                    return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true);
                })
                .map(llmResponse -> reportParser.parse(llmResponse, violations));
    }
}
