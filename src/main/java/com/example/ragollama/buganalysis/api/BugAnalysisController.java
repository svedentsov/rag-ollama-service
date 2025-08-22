package com.example.ragollama.buganalysis.api;

import com.example.ragollama.buganalysis.api.dto.BugAnalysisRequest;
import com.example.ragollama.buganalysis.api.dto.BugAnalysisResponse;
import com.example.ragollama.buganalysis.domain.BugAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * Контроллер для AI-агента, анализирующего баг-репорты.
 */
@RestController
@RequestMapping("/api/v1/agents/bug-analyzer")
@RequiredArgsConstructor
@Tag(name = "Bug Analysis Agent", description = "API для анализа баг-репортов на дубликаты и улучшения качества")
public class BugAnalysisController {

    private final BugAnalysisService bugAnalysisService;

    /**
     * Анализирует черновик баг-репорта, ищет дубликаты и предлагает улучшенное описание.
     *
     * @param request DTO с текстом черновика отчета.
     * @return {@link CompletableFuture} со структурированным результатом анализа.
     */
    @PostMapping
    @Operation(summary = "Проанализировать баг-репорт",
            description = "Принимает черновик описания бага, находит похожие существующие отчеты, " +
                    "использует LLM для определения дубликатов и улучшения исходного текста.")
    public CompletableFuture<BugAnalysisResponse> analyzeBugReport(@Valid @RequestBody BugAnalysisRequest request) {
        return bugAnalysisService.analyzeBugReport(request.draftDescription());
    }
}
