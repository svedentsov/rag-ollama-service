package com.example.ragollama.rag.api;

import com.example.ragollama.rag.domain.TestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

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
@Tag(name = "Test Case Agent", description = "API для поиска по тест-кейсам")
public class TestCaseController {

    private final TestCaseService testCaseService;

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
}