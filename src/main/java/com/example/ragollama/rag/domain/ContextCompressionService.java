package com.example.ragollama.rag.domain;

import com.example.ragollama.shared.config.properties.AppProperties;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.tokenization.TokenizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContextCompressionService {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final TokenizationService tokenizationService;
    private final AppProperties appProperties;

    /**
     * Сжимает список документов, если их общий объем превышает лимит токенов.
     *
     * @param documents Исходный список документов.
     * @param query     Запрос пользователя для определения релевантности.
     * @return Mono со списком документов (оригинальных или сжатых).
     */
    public Mono<List<Document>> compress(List<Document> documents, String query) {
        int totalTokens = documents.stream()
                .mapToInt(doc -> tokenizationService.countTokens(doc.getText()))
                .sum();
        if (totalTokens <= appProperties.context().maxTokens()) {
            return Mono.just(documents);
        }
        log.warn("Общий размер контекста ({}) превышает лимит ({}). Запуск сжатия...",
                totalTokens, appProperties.context().maxTokens());

        return Flux.fromIterable(documents)
                .flatMap(doc -> compressDocument(doc, query))
                .collectList();
    }

    private Mono<Document> compressDocument(Document document, String query) {
        String promptString = promptService.render("contextCompressorPrompt", Map.of(
                "query", query,
                "document_text", document.getText()
        ));
        Prompt prompt = new Prompt(promptString);
        return llmClient.callChat(prompt, ModelCapability.FASTEST)
                .map(compressedText -> new Document(compressedText, document.getMetadata()))
                .doOnSuccess(doc -> log.trace("Документ {} успешно сжат.", doc.getId()));
    }
}
