package com.example.ragollama.optimization;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.optimization.model.EnhancedMetadata;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEnhancerAgent implements ToolAgent {
    private final VectorStoreCurationRepository curationRepository;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "document-enhancer";
    }

    @Override
    public String getDescription() {
        return "Для документа генерирует саммари и ключевые слова, обновляя его метаданные.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("candidateIds");
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        List<UUID> candidateIds = (List<UUID>) context.payload().get("candidateIds");
        if (candidateIds == null || candidateIds.isEmpty()) {
            return CompletableFuture.completedFuture(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Нет документов для улучшения.", Map.of()));
        }

        return Flux.fromIterable(candidateIds)
                .flatMap(this::enhanceDocument)
                .collectList()
                .map(results -> new AgentResult(getName(), AgentResult.Status.SUCCESS, "Обработка " + results.size() + " документов завершена.", Map.of("processedDocs", results)))
                .toFuture();
    }

    private Mono<String> enhanceDocument(UUID docId) {
        String fullText = curationRepository.getFullTextByDocumentId(docId);
        if (fullText.isBlank()) {
            return Mono.just("SKIPPED (empty content)");
        }
        String promptString = promptService.render("documentEnhancerPrompt", Map.of("document_text", fullText));

        return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED))
                .map(this::parseLlmResponse)
                .flatMap(metadata -> {
                    Map<String, Object> updates = Map.of(
                            "summary", metadata.summary(),
                            "keywords", metadata.keywords(),
                            "last_curated_at", OffsetDateTime.now().toString()
                    );
                    return Mono.fromCallable(() -> {
                        curationRepository.updateMetadataByDocumentId(docId, updates);
                        log.info("Метаданные для документа {} успешно обновлены.", docId);
                        return "UPDATED";
                    });
                })
                .onErrorResume(e -> {
                    log.error("Ошибка при улучшении документа {}", docId, e);
                    return Mono.just("FAILED");
                });
    }

    private EnhancedMetadata parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = JsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, EnhancedMetadata.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("DocumentEnhancerAgent LLM вернул невалидный JSON.", e);
        }
    }
}
