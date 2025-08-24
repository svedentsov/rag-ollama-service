package com.example.ragollama.qaagent.api;

import com.example.ragollama.qaagent.AgentContext;
import com.example.ragollama.qaagent.AgentOrchestratorService;
import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.api.dto.BugAnalysisRequest;
import com.example.ragollama.qaagent.api.dto.BugAnalysisResponse;
import com.example.ragollama.qaagent.api.dto.BugReportSummaryRequest;
import com.example.ragollama.qaagent.domain.BugAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    private final AgentOrchestratorService orchestratorService;

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

    /**
     * Принимает "сырой" текст баг-репорта и преобразует его в структурированный формат.
     *
     * @param request DTO с текстом отчета от пользователя.
     * @return {@link CompletableFuture} с результатом, содержащим структурированный отчет.
     */
    @PostMapping("/summarize")
    @Operation(summary = "Структурировать и обобщить баг-репорт",
            description = "Принимает неструктурированный текст, описывающий проблему, и использует LLM для " +
                    "извлечения заголовка, шагов воспроизведения, ожидаемого и фактического результатов.")
    public CompletableFuture<List<AgentResult>> summarizeBugReport(@Valid @RequestBody BugReportSummaryRequest request) {
        AgentContext context = request.toAgentContext();
        return orchestratorService.invokePipeline("bug-report-summarization-pipeline", context);
    }
}
