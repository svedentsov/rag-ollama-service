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
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Специализированный сервис, инкапсулирующий логику анализа нарушений
 * доступности (a11y) с помощью языковой модели (LLM).
 * <p>
 * Этот класс является эталонной реализацией Принципа Единственной Ответственности (SRP).
 * Его единственная задача — принять список технических нарушений,
 * взаимодействовать с LLM для их анализа и обогащения, и вернуть
 * структурированный, человекочитаемый отчет.
 * <p>
 * Он полностью отделен от логики оркестрации агента, что упрощает его
 * тестирование в изоляции и переиспользование. Все операции выполняются асинхронно.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LlmAccessibilityAnalyzer {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final AccessibilityReportParser reportParser;
    private final AsyncTaskExecutor applicationTaskExecutor;

    /**
     * Асинхронно анализирует список технических нарушений доступности с помощью LLM.
     * <p>
     * Процесс включает в себя следующие асинхронные шаги:
     * <ol>
     *     <li>Сериализация списка нарушений в JSON (быстрая, синхронная операция).</li>
     *     <li>Формирование промпта для LLM с использованием шаблонизатора.</li>
     *     <li>Асинхронный вызов LLM для анализа JSON и генерации резюме и рекомендаций.</li>
     *     <li>Парсинг ответа LLM и его объединение с исходными данными для формирования финального отчета.</li>
     * </ol>
     *
     * @param violations Список технических нарушений, обнаруженных сканером.
     * @return {@link CompletableFuture}, который по завершении будет содержать
     * полностью сформированный {@link AccessibilityReport}.
     * @throws ProcessingException если происходит критическая ошибка при сериализации данных в JSON.
     */
    public CompletableFuture<AccessibilityReport> analyze(List<AccessibilityViolation> violations) {
        try {
            // Шаг 1: Сериализация JSON. Это быстрая, CPU-bound операция, нет нужды в supplyAsync.
            String violationsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(violations);
            String promptString = promptService.render("accessibilityAudit", Map.of("violationsJson", violationsJson));
            log.debug("Отправка запроса к LLM для анализа {} нарушений доступности.", violations.size());

            // Шаг 2: Асинхронный вызов LLM и последующая обработка.
            // thenApplyAsync гарантирует, что парсинг будет выполнен в том же пуле потоков.
            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .thenApplyAsync(llmResponse -> reportParser.parse(llmResponse, violations), applicationTaskExecutor);

        } catch (JsonProcessingException e) {
            log.error("Критическая ошибка: не удалось сериализовать список нарушений a11y в JSON.", e);
            // Возвращаем "проваленный" CompletableFuture
            return CompletableFuture.failedFuture(new ProcessingException("Ошибка сериализации нарушений a11y", e));
        }
    }
}
