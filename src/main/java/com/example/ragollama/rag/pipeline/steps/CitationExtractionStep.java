package com.example.ragollama.rag.pipeline.steps;

import com.example.ragollama.rag.domain.model.RagAnswer;
import com.example.ragollama.rag.domain.model.SourceCitation;
import com.example.ragollama.rag.pipeline.RagFlowContext;
import com.example.ragollama.rag.pipeline.RagPipelineStep;
import com.example.ragollama.shared.processing.PiiRedactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Шаг RAG-конвейера, отвечающий за извлечение цитат из сгенерированного ответа.
 * <p>
 * Этот шаг выполняется **после** генерации ответа LLM. Он парсит текст,
 * находит inline-цитаты (например, `[doc-id:1]`), сопоставляет их с исходными
 * документами и формирует точный список источников, а также очищает
 * финальный текст от служебных маркеров.
 */
@Component
@Order(50) // Выполняется после Generation
@RequiredArgsConstructor
@Slf4j
public class CitationExtractionStep implements RagPipelineStep {

    // Паттерн для поиска цитат формата [some-id:123]
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[([\\w\\-.:]+)]");
    private final PiiRedactionService piiRedactionService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<RagFlowContext> process(RagFlowContext context) {
        log.info("Шаг [50] Citation Extraction: парсинг цитат из ответа...");
        if (context.finalAnswer() == null || context.finalAnswer().answer() == null) {
            return Mono.just(context); // Пропускаем, если ответа нет
        }

        String rawAnswer = context.finalAnswer().answer();
        Map<String, Document> documentsById = context.rerankedDocuments().stream()
                .collect(Collectors.toMap(doc -> (String) doc.getMetadata().get("chunkId"), Function.identity()));

        Matcher matcher = CITATION_PATTERN.matcher(rawAnswer);

        List<SourceCitation> citations = matcher.results()
                .map(matchResult -> matchResult.group(1))
                .distinct()
                .map(documentsById::get)
                .filter(doc -> doc != null)
                .map(this::toSourceCitation)
                .toList();

        String cleanedAnswer = matcher.replaceAll("").trim();

        RagAnswer finalAnswer = new RagAnswer(cleanedAnswer, citations);
        log.debug("Извлечено {} уникальных цитат. Финальный ответ очищен.", citations.size());

        return Mono.just(context.withFinalAnswer(finalAnswer));
    }

    /**
     * Преобразует {@link Document} в DTO {@link SourceCitation}, применяя маскирование PII.
     *
     * @param doc Исходный документ-чанк.
     * @return DTO с цитатой, безопасное для отображения пользователю.
     */
    private SourceCitation toSourceCitation(Document doc) {
        return new SourceCitation(
                (String) doc.getMetadata().get("source"),
                piiRedactionService.redact(doc.getText()),
                doc.getMetadata(),
                (String) doc.getMetadata().get("chunkId"),
                (Float) doc.getMetadata().get("rerankedSimilarity")
        );
    }
}
