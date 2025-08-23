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
import reactor.core.publisher.Mono;

/**
 * Контроллер для AI-агента, анализирующего баг-репорты.
 * <p>
 * Эта версия использует полностью асинхронный подход на базе Project Reactor,
 * возвращая {@link Mono} для неблокирующей обработки.
 */
@RestController
@RequestMapping("/api/v1/agents/bug-analyzer")
@RequiredArgsConstructor
@Tag(name = "Bug Analysis Agent", description = "API для анализа баг-репортов на дубликаты и улучшения качества")
public class BugAnalysisController {

    private final BugAnalysisService bugAnalysisService;

    /**
     * Асинхронно анализирует черновик баг-репорта, ищет дубликаты и предлагает улучшенное описание.
     *
     * @param request DTO с текстом черновика отчета.
     * @return {@link Mono} со структурированным результатом анализа.
     * Ответ будет отправлен клиенту после асинхронного завершения операции.
     */
    @PostMapping
    @Operation(summary = "Проанализировать баг-репорт",
            description = "Принимает черновик описания бага, находит похожие существующие отчеты, " +
                    "использует LLM для определения дубликатов и улучшения исходного текста.")
    public Mono<BugAnalysisResponse> analyzeBugReport(@Valid @RequestBody BugAnalysisRequest request) {
        return bugAnalysisService.analyzeBugReport(request.draftDescription());
    }
}
