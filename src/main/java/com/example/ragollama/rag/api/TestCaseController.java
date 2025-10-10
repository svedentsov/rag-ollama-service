package com.example.ragollama.rag.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.testanalysis.api.dto.DeduplicationRequest;
import com.example.ragollama.rag.api.dto.ManualIndexRequest;
import com.example.ragollama.rag.domain.TestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Контроллер, предоставляющий API для специализированного поиска и индексации тест-кейсов.
 */
@RestController
@RequestMapping("/api/v1/test-cases")
@RequiredArgsConstructor
@Tag(name = "Test Case Agent", description = "API для поиска, индексации и анализа тест-кейсов")
public class TestCaseController {

    private final TestCaseService testCaseService;
    private final AgentOrchestratorService orchestratorService;

    /**
     * Выполняет семантический поиск по базе проиндексированных тест-кейсов.
     *
     * @param query Запрос на естественном языке.
     * @return {@link Mono} со списком найденных документов-тест-кейсов.
     */
    @GetMapping("/search")
    @Operation(summary = "Найти релевантные тест-кейсы по описанию (Finder)")
    public Mono<List<Document>> searchTestCases(@RequestParam @NotBlank String query) {
        return testCaseService.findRelevantTestCases(query);
    }

    /**
     * Принимает один тест-кейс и запускает его немедленную индексацию.
     *
     * @param request DTO с путем к файлу и его содержимым.
     * @return {@link ResponseEntity} со статусом 202 (Accepted).
     */
    @PostMapping("/index-manual")
    @Operation(summary = "Проиндексировать один тест-кейс вручную (Indexer)")
    @ApiResponse(responseCode = "202", description = "Задача на индексацию принята.")
    public ResponseEntity<Void> indexManually(@Valid @RequestBody ManualIndexRequest request) {
        Mono.fromRunnable(() -> testCaseService.indexManualTestCase(request)).subscribe();
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Запускает конвейер для поиска семантических дубликатов для заданного тест-кейса.
     *
     * @param request DTO с ID и контентом тест-кейса для анализа.
     * @return {@link Mono} с результатом, содержащим список найденных дубликатов.
     */
    @PostMapping("/find-duplicates")
    @Operation(summary = "Найти дубликаты для заданного тест-кейса")
    public Mono<List<AgentResult>> findDuplicates(@Valid @RequestBody DeduplicationRequest request) {
        return orchestratorService.invoke("test-case-deduplication-pipeline", request.toAgentContext());
    }
}
