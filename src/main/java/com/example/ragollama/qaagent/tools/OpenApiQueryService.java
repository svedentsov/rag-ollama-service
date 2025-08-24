package com.example.ragollama.qaagent.tools;

import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис-оркестратор, реализующий RAG-конвейер "на лету" для OpenAPI спецификаций.
 * <p>
 * Создает временное, изолированное векторное хранилище в памяти для каждой
 * анализируемой спецификации, обеспечивая высокую точность и производительность.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiQueryService {

    private final OpenApiSpecParser specParser;
    private final OpenApiChunker chunker;
    private final EmbeddingModel embeddingModel;
    private final LlmClient llmClient;
    private final PromptService promptService;

    /**
     * Выполняет RAG-запрос к спецификации, загруженной по URL.
     *
     * @param specUrl URL спецификации.
     * @param query   Запрос пользователя.
     * @return {@link CompletableFuture} с ответом.
     */
    public CompletableFuture<String> querySpecFromUrl(String specUrl, String query) {
        log.info("Загрузка и парсинг OpenAPI спецификации с URL: {}", specUrl);
        OpenAPI openAPI = specParser.parseFromUrl(specUrl);
        return executeRagPipeline(openAPI, query);
    }

    /**
     * Выполняет RAG-запрос к спецификации, переданной в виде текста.
     *
     * @param specContent Содержимое спецификации.
     * @param query       Запрос пользователя.
     * @return {@link CompletableFuture} с ответом.
     */
    public CompletableFuture<String> querySpecFromContent(String specContent, String query) {
        log.info("Парсинг OpenAPI спецификации из предоставленного контента.");
        OpenAPI openAPI = specParser.parseFromContent(specContent);
        return executeRagPipeline(openAPI, query);
    }

    private CompletableFuture<String> executeRagPipeline(OpenAPI openAPI, String query) {
        List<Document> chunks = chunker.split(openAPI);
        if (chunks.isEmpty()) {
            throw new ProcessingException("Не удалось извлечь контент из OpenAPI спецификации.");
        }
        log.debug("Спецификация разделена на {} чанков.", chunks.size());

        VectorStore inMemoryVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        inMemoryVectorStore.add(chunks);
        log.debug("Временное in-memory хранилище создано и заполнено.");

        SearchRequest searchRequest = SearchRequest.builder().query(query).topK(5).build();
        List<Document> similarDocs = inMemoryVectorStore.similaritySearch(searchRequest);
        log.debug("Найдено {} релевантных чанков для запроса.", similarDocs.size());

        String promptString = promptService.render("ragPrompt", Map.of(
                "documents", similarDocs,
                "question", query,
                "history", "Нет истории."
        ));

        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED);
    }
}
