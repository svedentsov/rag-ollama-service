package com.example.ragollama.monitoring;

import com.example.ragollama.monitoring.model.VerificationResult;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.metrics.MetricService;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Асинхронный сервис для пост-проверки сгенерированных RAG-ответов.
 * <p>
 * Использует LLM в роли "аудитора" для верификации того, что ответ
 * строго основан на предоставленном контексте и корректно цитирует источники.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SourceCiteVerifierService {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final MetricService metricService;
    private final ObjectMapper objectMapper;

    /**
     * Асинхронно выполняет проверку ответа.
     *
     * @param contextDocuments Документы, использованные как контекст для генерации.
     * @param generatedAnswer  Финальный ответ, сгенерированный RAG-системой.
     */
    @Async("applicationTaskExecutor")
    public void verify(List<Document> contextDocuments, String generatedAnswer) {
        if (contextDocuments == null || contextDocuments.isEmpty()) {
            log.debug("Верификация пропущена: контекст для генерации ответа был пуст.");
            return;
        }

        String contextAsString = contextDocuments.stream()
                .map(doc -> String.format("<doc source=\"%s\">\n%s\n</doc>",
                        doc.getMetadata().get("source"), doc.getText()))
                .collect(Collectors.joining("\n\n"));

        String promptString = promptService.render("sourceCiteVerifier", Map.of(
                "context", contextAsString,
                "answer", generatedAnswer
        ));

        llmClient.callChat(new Prompt(promptString), ModelCapability.FAST)
                .thenAccept(jsonResponse -> {
                    VerificationResult result = parseLlmResponse(jsonResponse);
                    metricService.recordVerificationResult(result.isValid());
                    if (!result.isValid()) {
                        log.warn("""
                                
                                !!! ПРОВЕРКА ЦИТИРОВАНИЯ НЕ ПРОЙДЕНА !!!
                                Ответ: "{}"
                                Причина: {}
                                Отсутствующие цитаты: {}
                                """, generatedAnswer, result.reasoning(), result.missingCitations());
                    } else {
                        log.info("Проверка цитирования успешно пройдена для ответа.");
                    }
                })
                .exceptionally(ex -> {
                    log.error("Ошибка во время выполнения асинхронной верификации ответа.", ex);
                    return null;
                });
    }

    /**
     * Надежно парсит ответ от LLM, используя {@link JsonExtractorUtil} для
     * извлечения чистого JSON-блока перед десериализацией.
     *
     * @param llmResponse Сырой ответ от LLM, который может содержать "мусор".
     * @return Десериализованный объект {@link VerificationResult}.
     */
    private VerificationResult parseLlmResponse(String llmResponse) {
        String cleanedJson = JsonExtractorUtil.extractJsonBlock(llmResponse);
        if (cleanedJson.isEmpty()) {
            log.error("Не удалось извлечь JSON из ответа LLM-верификатора: {}", llmResponse);
            return new VerificationResult(false, List.of(), "LLM вернула ответ без валидного JSON.");
        }
        try {
            return objectMapper.readValue(cleanedJson, VerificationResult.class);
        } catch (JsonProcessingException e) {
            log.error("Не удалось распарсить JSON-ответ от LLM-верификатора: {}", cleanedJson, e);
            return new VerificationResult(false, List.of(), "LLM вернула невалидный JSON.");
        }
    }
}
