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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * AI-агент ("Хранитель Знаний"), который проактивно ищет семантические
 * противоречия между документами в базе знаний.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeConsistencyGuardianAgent implements ToolAgent {

    private final VectorStore vectorStore;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "knowledge-consistency-guardian";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Проактивно ищет противоречия между похожими документами в базе знаний.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        List<Document> randomDocList = vectorStore.similaritySearch(SearchRequest.builder().query("*").topK(1).build());
        if (randomDocList.isEmpty()) {
            return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "База знаний пуста, проверка невозможна.", Map.of()));
        }
        Document docA = randomDocList.get(0);

        SearchRequest searchRequest = SearchRequest.builder().query(docA.getText()).topK(2).build();
        List<Document> neighbors = vectorStore.similaritySearch(searchRequest);
        Document docB = neighbors.stream().filter(d -> !d.getId().equals(docA.getId())).findFirst().orElse(null);

        if (docB == null) {
            return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Не найдены соседи для документа, проверка пропущена.", Map.of()));
        }

        log.info("Проверка на противоречия между документами: {} и {}", docA.getMetadata().get("source"), docB.getMetadata().get("source"));
        String promptString = promptService.render("consistencyGuardianPrompt", Map.of(
                "docA_id", docA.getMetadata().get("chunkId"),
                "docA_text", docA.getText(),
                "docB_id", docB.getMetadata().get("chunkId"),
                "docB_text", docB.getText()
        ));

        final Document finalDocB = docB;
        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED, true)
                .map(tuple -> parseLlmResponse(tuple.getT1()))
                .map(result -> {
                    if (result.isContradictory()) {
                        log.warn("!!! ОБНАРУЖЕНО ПРОТИВОРЕЧИЕ: {}", result.justification());
                        return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Обнаружено противоречие.", Map.of("contradictionDetails", result, "docA", docA, "docB", finalDocB));
                    }
                    return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Противоречий не найдено.", Map.of());
                });
    }

    private ContradictionResult parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, ContradictionResult.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("Consistency Guardian LLM вернул невалидный JSON.", e);
        }
    }
}
