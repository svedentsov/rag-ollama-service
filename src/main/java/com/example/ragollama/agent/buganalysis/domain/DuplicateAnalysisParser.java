package com.example.ragollama.agent.buganalysis.domain;

import com.example.ragollama.agent.buganalysis.model.BugAnalysisReport;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Компонент-парсер, отвечающий за преобразование "сырого" строкового
 * ответа от LLM в строго типизированный DTO {@link BugAnalysisReport}.
 * <p>
 * Изоляция этой логики в отдельном классе повышает тестируемость и
 * соответствует Принципу единственной ответственности.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DuplicateAnalysisParser {

    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * Надежно парсит JSON-ответ от LLM.
     *
     * @param llmResponse Сырой ответ от языковой модели.
     * @return Десериализованный объект {@link BugAnalysisReport}.
     * @throws ProcessingException если парсинг не удался.
     */
    public BugAnalysisReport parse(String llmResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(llmResponse);
            return objectMapper.readValue(cleanedJson, BugAnalysisReport.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM даже после очистки: {}", llmResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON-ответ.", e);
        }
    }
}