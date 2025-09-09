package com.example.ragollama.rag.domain.reranking;

import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Стратегия переранжирования, использующая LLM для оценки релевантности.
 * <p>
 * Принимает список документов-кандидатов и использует языковую модель
 * для их оценки и сортировки в порядке убывания релевантности
 * относительно исходного запроса.
 * <p>
 * Активируется свойством {@code app.reranking.strategies.llm.enabled=true}.
 */
@Slf4j
@Component
@Order(20) // Выполняется после базовых стратегий
@ConditionalOnProperty(name = "app.reranking.strategies.llm.enabled", havingValue = "true")
@RequiredArgsConstructor
public class LlmRerankerStrategy implements RerankingStrategy {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * DTO для внутреннего использования при парсинге ответа от LLM-реранкера.
     *
     * @param chunkId          Идентификатор чанка.
     * @param relevanceScore   Оценка релевантности (1-10), присвоенная LLM.
     * @param justification    Обоснование оценки.
     */
    private record RankedDocument(String chunkId, int relevanceScore, String justification) {
    }

    /**
     * {@inheritDoc}
     *
     * @param documents     Список документов для обработки.
     * @param originalQuery Оригинальный запрос пользователя.
     * @return Список документов с обновленными метаданными.
     */
    @Override
    public List<Document> apply(List<Document> documents, String originalQuery) {
        if (documents == null || documents.size() < 2) {
            return documents;
        }

        log.debug("Применение LlmRerankerStrategy для {} документов.", documents.size());

        // Преобразуем документы в простой формат для передачи в LLM
        String contextForLlm = documents.stream()
                .map(doc -> String.format("<doc id=\"%s\">\n%s\n</doc>",
                        doc.getMetadata().get("chunkId"),
                        doc.getText()))
                .collect(Collectors.joining("\n\n"));

        String promptString = promptService.render("llmRerankerPrompt", Map.of(
                "query", originalQuery,
                "documents", contextForLlm
        ));

        try {
            // Выполняем синхронный блокирующий вызов, так как реранжирование - критический шаг
            String jsonResponse = llmClient.callChat(new Prompt(promptString), ModelCapability.FAST_RELIABLE).join();
            List<RankedDocument> rankedResults = parseLlmResponse(jsonResponse);

            // Создаем карту для быстрого доступа к документам по ID
            Map<String, Document> docMap = documents.stream()
                    .collect(Collectors.toMap(doc -> (String) doc.getMetadata().get("chunkId"), Function.identity()));

            // Собираем и сортируем финальный список
            return rankedResults.stream()
                    .peek(rankedDoc -> {
                        Document doc = docMap.get(rankedDoc.chunkId());
                        if (doc != null) {
                            // Обновляем оценку релевантности на основе вердикта LLM
                            doc.getMetadata().put("rerankedSimilarity", (float) rankedDoc.relevanceScore() / 10.0f);
                        }
                    })
                    .map(rankedDoc -> docMap.get(rankedDoc.chunkId()))
                    .filter(doc -> doc != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Ошибка при выполнении LLM-реранжирования. Возвращается исходный порядок документов.", e);
            return documents;
        }
    }

    /**
     * Безопасно парсит JSON-ответ от LLM.
     *
     * @param jsonResponse Ответ от LLM.
     * @return Список объектов {@link RankedDocument}.
     * @throws ProcessingException если парсинг не удался.
     */
    private List<RankedDocument> parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ProcessingException("LlmReranker LLM вернул невалидный JSON.", e);
        }
    }
}
