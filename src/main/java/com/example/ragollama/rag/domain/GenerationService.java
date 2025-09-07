package com.example.ragollama.rag.domain;

import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.domain.generation.NoContextStrategy;
import com.example.ragollama.rag.domain.model.RagAnswer;
import com.example.ragollama.rag.domain.model.SourceCitation;
import com.example.ragollama.shared.exception.GenerationException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.processing.PiiRedactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Сервис, отвечающий за этап генерации ответа в RAG-конвейере.
 * <p>
 * Эта версия формирует богатый доменный объект {@link RagAnswer}, который
 * включает в себя не просто список имен источников, а полный, структурированный
 * список цитат {@link SourceCitation} с текстом и метаданными.
 * Ключевое изменение: применяется маскирование PII к фрагментам текста
 * *перед* их возвратом (Egress Redaction).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    private final LlmClient llmClient;
    private final NoContextStrategy noContextStrategy;
    private final PiiRedactionService piiRedactionService;

    /**
     * Асинхронно генерирует полный ответ.
     *
     * @param prompt    Промпт для LLM.
     * @param documents Документы, использованные в контексте.
     * @return {@link CompletableFuture} с {@link RagAnswer}.
     */
    public CompletableFuture<RagAnswer> generate(Prompt prompt, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.warn("На этап Generation не передано документов. Применяется стратегия '{}'.", noContextStrategy.getClass().getSimpleName());
            return noContextStrategy.handle(prompt).toFuture();
        }
        return llmClient.callChat(prompt, ModelCapability.BALANCED)
                .thenApply(generatedAnswer -> {
                    List<SourceCitation> sourceCitations = extractCitations(documents);
                    return new RagAnswer(generatedAnswer, sourceCitations);
                })
                .exceptionally(ex -> {
                    log.error("Ошибка на этапе генерации ответа LLM", ex);
                    throw new GenerationException("Не удалось сгенерировать ответ от LLM.", ex);
                });
    }

    /**
     * Генерирует ответ в виде структурированного потока.
     *
     * @param prompt    Промпт для LLM.
     * @param documents Документы, использованные в контексте.
     * @return {@link Flux} со {@link StreamingResponsePart}.
     */
    public Flux<StreamingResponsePart> generateStructuredStream(Prompt prompt, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.warn("В потоковом запросе на этап Generation не передано документов.");
            return noContextStrategy.handle(prompt)
                    .flatMapMany(answer -> Flux.just(
                            new StreamingResponsePart.Content(answer.answer()),
                            new StreamingResponsePart.Done("Завершено без контекста")
                    ));
        }

        Flux<StreamingResponsePart> contentStream = llmClient.streamChat(prompt, ModelCapability.BALANCED)
                .map(StreamingResponsePart.Content::new);

        Flux<StreamingResponsePart> tailStream = Flux.just(
                new StreamingResponsePart.Sources(extractCitations(documents)),
                new StreamingResponsePart.Done("Успешно завершено")
        );

        return Flux.concat(contentStream, tailStream)
                .doOnError(ex -> log.error("Ошибка в потоке генерации ответа LLM", ex))
                .onErrorResume(ex -> {
                    String errorMessage = "Ошибка при генерации ответа: " + ex.getMessage();
                    return Flux.just(new StreamingResponsePart.Error(errorMessage));
                });
    }

    /**
     * Преобразует список {@link Document} в список {@link SourceCitation},
     * применяя маскирование PII к текстовым фрагментам.
     *
     * @param documents Документы, использованные в контексте.
     * @return Список структурированных и безопасных для отображения цитат.
     */
    private List<SourceCitation> extractCitations(List<Document> documents) {
        if (documents == null) {
            return Collections.emptyList();
        }
        return documents.stream()
                .map(doc -> new SourceCitation(
                        (String) doc.getMetadata().get("source"),
                        piiRedactionService.redact(doc.getText()), // Egress Redaction
                        doc.getMetadata(),
                        (String) doc.getMetadata().get("chunkId"),
                        (Float) doc.getMetadata().get("rerankedSimilarity")
                ))
                .distinct()
                .collect(Collectors.toList());
    }
}
