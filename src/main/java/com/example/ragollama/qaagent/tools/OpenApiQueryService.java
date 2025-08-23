package com.example.ragollama.qaagent.tools;

import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
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
import java.util.stream.Collectors;

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
     */
    public CompletableFuture<String> querySpecFromUrl(String specUrl, String query) {
        log.info("Загрузка и парсинг OpenAPI спецификации с URL: {}", specUrl);
        OpenAPI openAPI = specParser.parseFromUrl(specUrl);
        return executeRagPipeline(openAPI, query);
    }

    /**
     * Выполняет RAG-запрос к спецификации, переданной в виде текста.
     */
    public CompletableFuture<String> querySpecFromContent(String specContent, String query) {
        log.info("Парсинг OpenAPI спецификации из предоставленного контента.");
        OpenAPI openAPI = specParser.parseFromContent(specContent);
        return executeRagPipeline(openAPI, query);
    }

    /**
     * Основная логика RAG-конвейера.
     */
    private CompletableFuture<String> executeRagPipeline(OpenAPI openAPI, String query) {
        // Шаг 1: Разбиваем спецификацию на текстовые чанки
        List<Document> chunks = chunker.split(openAPI);
        if (chunks.isEmpty()) {
            throw new ProcessingException("Не удалось извлечь контент из OpenAPI спецификации.");
        }
        log.debug("Спецификация разделена на {} чанков.", chunks.size());

        // Шаг 2: Создаем временное in-memory векторное хранилище (актуальный способ)
        VectorStore inMemoryVectorStore = SimpleVectorStore
                .builder(embeddingModel)
                .build();
        inMemoryVectorStore.add(chunks);
        log.debug("Временное in-memory хранилище создано и заполнено.");

        // Шаг 3: Выполняем поиск по схожести (актуальные методы builder'а)
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(5)
                .build();

        List<Document> similarDocs = inMemoryVectorStore.similaritySearch(searchRequest);
        log.debug("Найдено {} релевантных чанков для запроса.", similarDocs.size());

        // Шаг 4: Формируем контекст и промпт
        String context = similarDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        String promptString = promptService.render("ragPrompt", Map.of(
                "documents", similarDocs,
                "question", query,
                "history", "Нет истории."
        ));

        // Шаг 5: Вызываем LLM для генерации ответа
        return llmClient.callChat(new Prompt(promptString));
    }
}
