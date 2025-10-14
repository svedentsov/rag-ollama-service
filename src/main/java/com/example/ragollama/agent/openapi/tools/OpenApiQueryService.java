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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Сервис-оркестратор, реализующий RAG-конвейер "на лету" для OpenAPI спецификаций.
 * <p>
 * Этот сервис инкапсулирует весь сложный процесс:
 * <ol>
 *     <li>Парсинг OpenAPI спецификации из любого источника.</li>
 *     <li>Динамическое разбиение спецификации на семантические чанки.</li>
 *     <li>Создание временного векторного индекса в памяти.</li>
 *     <li>Выполнение семантического поиска по этому индексу.</li>
 *     <li>Сборка контекста и вызов LLM для генерации ответа.</li>
 * </ol>
 * Все операции выполняются асинхронно с использованием Project Reactor.
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
     * Выполняет полный RAG-запрос к спецификации из любого источника.
     *
     * @param source Источник спецификации (URL или контент).
     * @param query  Запрос пользователя на естественном языке.
     * @return {@link Mono} с текстовым ответом, сгенерированным LLM.
     */
    public Mono<String> querySpec(OpenApiSourceRequest source, String query) {
        log.info("Парсинг OpenAPI спецификации из источника типа: {}", source.getClass().getSimpleName());
        OpenAPI openAPI = specParser.parse(source);
        return executeRagPipeline(openAPI, query);
    }

    /**
     * Выполняет полный RAG-конвейер "на лету".
     *
     * @param openApi Распарсенный объект OpenAPI.
     * @param query   Вопрос пользователя.
     * @return {@link Mono} с финальным ответом.
     */
    private Mono<String> executeRagPipeline(OpenAPI openApi, String query) {
        // 1. Динамическое разбиение на чанки
        List<Document> chunks = chunker.split(openApi);
        if (chunks.isEmpty()) {
            return Mono.error(new ProcessingException("Не удалось извлечь контент из OpenAPI спецификации."));
        }
        log.debug("Спецификация разделена на {} чанков.", chunks.size());

        // 2. Создание временного векторного индекса в памяти
        VectorStore inMemoryVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        inMemoryVectorStore.add(chunks);
        log.debug("Временное in-memory хранилище создано и заполнено.");

        // 3. Семантический поиск
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(5)
                .build();
        List<Document> similarDocs = inMemoryVectorStore.similaritySearch(searchRequest);
        log.debug("Найдено {} релевантных чанков для запроса.", similarDocs.size());

        // 4. Сборка промпта и генерация ответа
        String promptString = promptService.render("ragPrompt", Map.of(
                "documents", similarDocs,
                "question", query,
                "history", "Нет истории."
        ));
        return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .map(tuple -> tuple.getT1());
    }
}
