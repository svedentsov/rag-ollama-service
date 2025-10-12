package com.example.ragollama.agent.accessibility.domain;

import com.example.ragollama.agent.accessibility.model.AccessibilityReport;
import com.example.ragollama.agent.accessibility.model.AccessibilityViolation;
import com.example.ragollama.shared.exception.LlmJsonResponseParseException;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Компонент-парсер, отвечающий за безопасное преобразование JSON-ответа от LLM
 * в строго типизированный DTO {@link AccessibilityReport}.
 * <p>
 * Изоляция этой логики в отдельном классе повышает тестируемость, отказоустойчивость
 * и соответствует Принципу единственной ответственности (SRP).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessibilityReportParser {

    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * Безопасно парсит JSON-ответ от LLM, очищает его и объединяет с исходными данными.
     * <p>
     * Этот метод является "антикоррупционным слоем" между непредсказуемым выводом LLM
     * и строго типизированной доменной моделью приложения.
     *
     * @param jsonResponse  Сырой строковый JSON-ответ от языковой модели.
     * @param rawViolations Исходный список нарушений от сканера. Этот список
     *                      является "источником правды" для технических деталей.
     * @return Полностью собранный и валидный {@link AccessibilityReport}, где
     * резюме взято от LLM, а технические детали — из исходных данных.
     * @throws LlmJsonResponseParseException если LLM вернула невалидный JSON, который
     *                                       не удалось распарсить даже после очистки.
     */
    public AccessibilityReport parse(String jsonResponse, List<AccessibilityViolation> rawViolations) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            AccessibilityReport summaryReport = objectMapper.readValue(cleanedJson, AccessibilityReport.class);
            log.debug("JSON-ответ от LLM для аудита доступности успешно распарсен.");

            // Собираем финальный отчет: саммари и рекомендации от LLM, а "сырые" данные — из сканера.
            return new AccessibilityReport(summaryReport.summary(), summaryReport.topRecommendations(), rawViolations);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM-аудитора доступности: {}", jsonResponse, e);
            throw new LlmJsonResponseParseException("LLM-аудитор вернул невалидный JSON.", e, jsonResponse);
        }
    }
}
