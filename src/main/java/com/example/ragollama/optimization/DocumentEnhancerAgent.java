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

/**
 * Агент для обогащения документов метаданными (summary, keywords).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEnhancerAgent implements ToolAgent {

    private final VectorStoreCurationRepository curationRepository;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final JsonExtractorUtil jsonExtractorUtil;

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
        return context.payload().containsKey("candidateIds") || context.payload().containsKey("document_text");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<AgentResult> execute(AgentContext context) {
        if (context.payload().containsKey("document_text")) {
            return enhanceSingleDocument(context);
        }

        List<UUID> candidateIds = (List<UUID>) context.payload().get("candidateIds");
        if (candidateIds == null || candidateIds.isEmpty()) {
            return Mono.just(new AgentResult(getName(), AgentResult.Status.SUCCESS, "Нет документов для улучшения.", Map.of()));
        }

        return Flux.fromIterable(candidateIds)
                .flatMap(this::enhanceDocumentInDb)
                .collectList()
                .map(results -> new AgentResult(getName(), AgentResult.Status.SUCCESS, "Обработка " + results.size() + " документов завершена.", Map.of("processedDocs", results)));
    }

    private Mono<AgentResult> enhanceSingleDocument(AgentContext context) {
        String fullText = (String) context.payload().get("document_text");
        String promptString = promptService.render("documentEnhancerPrompt", Map.of("document_text", fullText));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .map(this::parseLlmResponse)
                .map(metadata -> new AgentResult(
                        getName(),
                        AgentResult.Status.SUCCESS,
                        "Метаданные успешно сгенерированы.",
                        Map.of("enhancedMetadata", metadata)
                ));
    }

    private Mono<String> enhanceDocumentInDb(UUID docId) {
        return curationRepository.getFullTextByDocumentId(docId)
                .flatMap(fullText -> {
                    if (fullText.isBlank()) {
                        return Mono.just("SKIPPED (empty content)");
                    }
                    String promptString = promptService.render("documentEnhancerPrompt", Map.of("document_text", fullText));

                    return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                            .map(this::parseLlmResponse)
                            .flatMap(metadata -> {
                                Map<String, Object> updates = Map.of(
                                        "summary", metadata.summary(),
                                        "keywords", metadata.keywords(),
                                        "last_curated_at", OffsetDateTime.now().toString()
                                );
                                return curationRepository.updateMetadataByDocumentId(docId, updates)
                                        .doOnSuccess(count -> log.info("Метаданные для документа {} успешно обновлены ({} чанков).", docId, count))
                                        .thenReturn("UPDATED");
                            });
                })
                .onErrorResume(e -> {
                    log.error("Ошибка при улучшении документа {}", docId, e);
                    return Mono.just("FAILED");
                });
    }

    private EnhancedMetadata parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, EnhancedMetadata.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("DocumentEnhancerAgent LLM вернул невалидный JSON.", e);
        }
    }
}
