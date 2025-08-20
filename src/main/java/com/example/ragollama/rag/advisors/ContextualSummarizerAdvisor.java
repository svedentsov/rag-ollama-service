package com.example.ragollama.rag.advisors;

import com.example.ragollama.rag.model.RagContext;
import com.example.ragollama.shared.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * Советник (Advisor), реализующий логику "Summarizer Agent".
 * <p>
 * Этот компонент перехватывает список извлеченных документов и для каждого из них
 * генерирует краткую выжимку (summary) в контексте оригинального запроса пользователя.
 * Это позволяет значительно сократить объем "шума" и количество токенов,
 * передаваемых в основную LLM, повышая точность и снижая стоимость генерации.
 * *
 * Активируется свойством {@code app.rag.summarizer.enabled=true}.
 */
@Slf4j
@Component
@Order(30) // Выполняется после других советников, работающих с полным текстом.
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rag.summarizer.enabled", havingValue = "true")
public class ContextualSummarizerAdvisor implements RagAdvisor {

    private final LlmClient llmClient;

    private static final PromptTemplate SUMMARIZER_PROMPT_TEMPLATE = new PromptTemplate("""
            Твоя задача — сделать очень краткую выжимку из предоставленного ТЕКСТА,
            сосредоточившись ИСКЛЮЧИТЕЛЬНО на информации, которая напрямую отвечает на ВОПРОС.
            Игнорируй всю информацию в тексте, не относящуюся к вопросу.
            Ответ должен быть сжатым, фактическим и на русском языке.
            ---
            ВОПРОС: {query}
            ---
            ТЕКСТ:
            {document_text}
            """);

    /**
     * Асинхронно применяет свою логику к контексту RAG-запроса.
     * <p>
     * Асинхронно и параллельно обрабатывает все документы, заменяя их содержимое
     * на контекстно-зависимые выжимки.
     *
     * @param context Текущий контекст запроса, содержащий документы,
     *                оригинальный запрос и модель промпта.
     * @return {@link Mono}, который по завершении будет содержать
     * модифицированный контекст для следующего советника в цепочке.
     */
    @Override
    public Mono<RagContext> advise(RagContext context) {
        if (context.getDocuments() == null || context.getDocuments().isEmpty()) {
            return Mono.just(context);
        }

        log.info("ContextualSummarizerAdvisor: начало сжатия {} документов.", context.getDocuments().size());

        return Flux.fromIterable(context.getDocuments())
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(doc -> summarizeDocument(doc, context.getOriginalQuery()))
                .sequential()
                .collectList()
                .map(summarizedDocs -> {
                    context.setDocuments(summarizedDocs);
                    log.info("ContextualSummarizerAdvisor: сжатие завершено.");
                    return context;
                });
    }

    /**
     * Асинхронно генерирует выжимку для одного документа.
     *
     * @param originalDoc Оригинальный документ.
     * @param query       Оригинальный запрос пользователя для контекста.
     * @return {@link Mono}, содержащий новый {@link Document} с выжимкой вместо полного текста.
     * В случае ошибки возвращает оригинальный документ.
     */
    private Mono<Document> summarizeDocument(Document originalDoc, String query) {
        String promptString = SUMMARIZER_PROMPT_TEMPLATE.render(Map.of(
                "query", query,
                "document_text", originalDoc.getText()
        ));
        Prompt prompt = new Prompt(promptString);

        return Mono.fromFuture(llmClient.callChat(prompt))
                .map(summary -> new Document(summary, originalDoc.getMetadata()))
                .doOnError(e -> log.warn("Не удалось сжать документ ID {}: {}", originalDoc.getId(), e.getMessage()))
                .onErrorReturn(originalDoc); // В случае ошибки возвращаем оригинальный документ
    }
}
