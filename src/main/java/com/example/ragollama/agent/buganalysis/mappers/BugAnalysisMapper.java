package com.example.ragollama.agent.buganalysis.mappers;

import com.example.ragollama.agent.buganalysis.api.dto.BugAnalysisResponse;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Компонент-маппер, отвечающий за преобразование "сырого" строкового
 * ответа от LLM в строго типизированный DTO {@link BugAnalysisResponse}.
 * <p>
 * Изоляция этой логики в отдельном классе повышает тестируемость и
 * соответствует Принципу единственной ответственности.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BugAnalysisMapper {

    private final ObjectMapper objectMapper;

    /**
     * Надежно парсит JSON-ответ от LLM, предварительно очищая его от
     * артефактов (Markdown, лишние пробелы) с помощью {@link JsonExtractorUtil}.
     *
     * @param llmResponse Сырой ответ от языковой модели.
     * @return Десериализованный объект {@link BugAnalysisResponse}.
     * @throws ProcessingException если парсинг не удался даже после очистки.
     */
    public BugAnalysisResponse parse(String llmResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(llmResponse);
            return objectMapper.readValue(cleanedJson, BugAnalysisResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM даже после очистки: {}", llmResponse, e);
            throw new ProcessingException("LLM вернула невалидный JSON-ответ.", e);
        }
    }
}
