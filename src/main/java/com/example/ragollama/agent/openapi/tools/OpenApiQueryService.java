package com.example.ragollama.agent.openapi.tools;

import com.example.ragollama.agent.openapi.api.dto.OpenApiSourceRequest;
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
 * <p>Создает временное, изолированное векторное хранилище в памяти для каждой
 * анализируемой спецификации, обеспечивая высокую точность и производительность.
 * Эта версия адаптирована для работы с полиморфным источником {@link OpenApiSourceRequest}.
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
     * Выполняет RAG-запрос к спецификации из любого источника.
     *
     * @param source Источник спецификации (URL или контент).
     * @param query  Запрос пользователя.
     * @return {@link CompletableFuture} с ответом.
     */
    public CompletableFuture<String> querySpec(OpenApiSourceRequest source, String query) {
        log.info("Парсинг OpenAPI спецификации из источника типа: {}", source.getClass().getSimpleName());
        OpenAPI openAPI = specParser.parse(source);
        return executeRagPipeline(openAPI, query);
    }

    /**
     * Выполняет полный RAG-конвейер: чанкинг, индексация в памяти, поиск и генерация.
     *
     * @param openAPI Распарсенный объект спецификации.
     * @param query   Запрос пользователя.
     * @return {@link CompletableFuture} с ответом от LLM.
     */
    private CompletableFuture<String> executeRagPipeline(OpenAPI openAPI, String query) {
        List<Document> chunks = chunker.split(openAPI);
        if (chunks.isEmpty()) {
            throw new ProcessingException("Не удалось извлечь контент из OpenAPI спецификации.");
        }
        log.debug("Спецификация разделена на {} чанков.", chunks.size());
        VectorStore inMemoryVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        inMemoryVectorStore.add(chunks);
        log.debug("Временное in-memory хранилище создано и заполнено.");
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(5)
                .build();
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
