package com.example.ragollama.rag.api;

import com.example.ragollama.qaagent.AgentOrchestratorService;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.api.dto.DeduplicationRequest;
import com.example.ragollama.rag.domain.TestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Контроллер, предоставляющий API для специализированного поиска по базе тест-кейсов.
 * <p>
 * Этот эндпоинт позволяет пользователям искать релевантные тест-кейсы,
 * используя запросы на естественном языке.
 */
@RestController
@RequestMapping("/api/v1/test-cases")
@RequiredArgsConstructor
@Tag(name = "Test Case Agent", description = "API для поиска и анализа тест-кейсов")
public class TestCaseController {

    private final TestCaseService testCaseService;
    private final AgentOrchestratorService orchestratorService;

    /**
     * Выполняет семантический поиск по базе проиндексированных тест-кейсов.
     *
     * @param query Запрос на естественном языке (например, "тесты для формы авторизации").
     * @return {@link CompletableFuture}, который по завершении будет содержать список найденных документов-тест-кейсов.
     */
    @GetMapping("/search")
    @Operation(summary = "Найти релевантные тест-кейсы по описанию",
            description = "Выполняет гибридный поиск по базе знаний, отфильтрованной только по документам типа 'test_case'.")
    public CompletableFuture<List<Document>> searchTestCases(@RequestParam @NotBlank String query) {
        return testCaseService.findRelevantTestCases(query).toFuture();
    }

    /**
     * Запускает конвейер для поиска семантических дубликатов для заданного тест-кейса.
     *
     * @param request DTO с ID и контентом тест-кейса для анализа.
     * @return {@link CompletableFuture} с результатом, содержащим список найденных дубликатов.
     */
    @PostMapping("/find-duplicates")
    @Operation(summary = "Найти дубликаты для заданного тест-кейса",
            description = "Выполняет двухступенчатый анализ: сначала быстрый семантический поиск кандидатов, затем точную верификацию каждой пары с помощью LLM.")
    public CompletableFuture<List<AgentResult>> findDuplicates(@Valid @RequestBody DeduplicationRequest request) {
        return orchestratorService.invokePipeline("test-case-deduplication-pipeline", request.toAgentContext());
    }
}
