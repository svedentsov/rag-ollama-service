package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.optimization.model.ContradictionResult;
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
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeConsistencyGuardianAgent implements ToolAgent {

    private final VectorStore vectorStore;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "knowledge-consistency-guardian";
    }

    @Override
    public String getDescription() {
        return "Проактивно ищет противоречия между похожими документами в базе знаний.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return true; // Запускается без внешнего контекста
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        // 1. Выбираем случайный документ как отправную точку
        // ИСПРАВЛЕНИЕ: Используем .query() вместо .withQuery()
        List<Document> randomDocList = vectorStore.similaritySearch(SearchRequest.builder().query("*").topK(1).build());
        if (randomDocList.isEmpty()) {
            return CompletableFuture.completedFuture(new AgentResult(getName(), AgentResult.Status.SUCCESS, "База знаний пуста, проверка невозможна.", Map.of()));
        }
        Document docA = randomDocList.get(0);

        // 2. Находим его ближайшего семантического соседа
        // ИСПРАВЛЕНИЕ: Используем .query() вместо .withQuery()
        SearchRequest searchRequest = SearchRequest.builder().query(docA.getText()).topK(2).build();
        List<Document> neighbors = vectorStore.similaritySearch(searchRequest);
        Document docB = neighbors.stream().filter(d -> !d.getId().equals(docA.getId())).findFirst().orElse(null);

        if (docB == null) {
            return CompletableFuture.completedFuture(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Не найдены соседи для документа, проверка пропущена.", Map.of()));
        }

        // 3. Отправляем пару на проверку в LLM
        log.info("Проверка на противоречия между документами: {} и {}", docA.getMetadata().get("source"), docB.getMetadata().get("source"));
        String promptString = promptService.render("consistencyGuardianPrompt", Map.of(
                "docA_id", docA.getMetadata().get("chunkId"),
                "docA_text", docA.getText(),
                "docB_id", docB.getMetadata().get("chunkId"),
                "docB_text", docB.getText()
        ));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .thenApply(this::parseLlmResponse)
                .thenApply(result -> {
                    if (result.isContradictory()) {
                        log.warn("!!! ОБНАРУЖЕНО ПРОТИВОРЕЧИЕ: {}", result.justification());
                        return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Обнаружено противоречие.", Map.of("contradictionDetails", result, "docA", docA, "docB", docB));
                    }
                    return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Противоречий не найдено.", Map.of());
                });
    }

    private ContradictionResult parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ContradictionResult.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Consistency Guardian LLM вернул невалидный JSON.", e);
        }
    }
}
