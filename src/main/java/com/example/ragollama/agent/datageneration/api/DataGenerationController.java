package com.example.ragollama.agent.datageneration.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.datageneration.api.dto.DataGenerationRequest;
import com.example.ragollama.agent.datageneration.api.dto.DataSubsetRequest;
import com.example.ragollama.agent.datageneration.api.dto.SyntheticDataDpRequest;
import com.example.ragollama.agent.datageneration.api.dto.SyntheticDataRequest;
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

/**
 * Контроллер для управления AI-агентами, генерирующими данные.
 */
@RestController
@RequestMapping("/api/v1/agents/data")
@RequiredArgsConstructor
@Tag(name = "Data Generation Agents", description = "API для генерации синтетических данных и моков")
public class DataGenerationController {

    private final AgentOrchestratorService orchestratorService;

    /**
     * Запускает агента для генерации синтетических данных.
     *
     * @param request DTO с определением Java-класса и количеством моков.
     * @return {@link Mono} с результатом, содержащим сгенерированный JSON.
     */
    @PostMapping("/generate-mock")
    @Operation(summary = "Сгенерировать моковые данные для Java-класса",
            description = "Принимает исходный код Java DTO или Entity и генерирует " +
                    "указанное количество JSON-объектов с реалистичными данными.")
    public Mono<List<AgentResult>> generateMockData(@Valid @RequestBody SyntheticDataRequest request) {
        return orchestratorService.invoke(
                "synthetic-data-generation-pipeline", request.toAgentContext()
        );
    }

    /**
     * Запускает агента для создания подмножества данных из БД с маскированием PII.
     *
     * @param request DTO со схемой таблицы и целью выборки.
     * @return {@link Mono} с результатом, содержащим сгенерированный SQL и замаскированные данные.
     */
    @PostMapping("/create-subset")
    @Operation(summary = "Создать и замаскировать подмножество данных из БД",
            description = "Принимает DDL таблицы и цель на естественном языке. AI генерирует SQL, " +
                    "который выполняется после вашего одобрения, а результат маскируется.")
    public Mono<List<AgentResult>> createDataSubset(@Valid @RequestBody DataSubsetRequest request) {
        return orchestratorService.invoke("data-subset-masking-pipeline", request.toAgentContext());
    }

    /**
     * Запускает конвейер для создания синтетических данных с дифференциальной приватностью.
     *
     * @param request DTO с SQL-запросом для исходных данных и параметрами генерации.
     * @return {@link Mono} с финальным отчетом, содержащим данные.
     */
    @PostMapping("/create-dp-subset")
    @Operation(summary = "Создать синтетические данные с дифференциальной приватностью (DP)")
    public Mono<List<AgentResult>> createDpDataSubset(@Valid @RequestBody SyntheticDataDpRequest request) {
        return orchestratorService.invoke("dp-synthetic-data-pipeline", request.toAgentContext());
    }

    /**
     * Запускает агента для генерации статистически-релевантных синтетических данных.
     *
     * @param request DTO с SQL-запросом для исходных данных и количеством записей.
     * @return {@link Mono} с отчетом, содержащим сгенерированные данные.
     */
    @PostMapping("/generate-statistical-data")
    @Operation(summary = "Сгенерировать статистически-релевантные данные")
    public Mono<List<AgentResult>> generateStatisticalData(@Valid @RequestBody DataGenerationRequest request) {
        return orchestratorService.invoke("statistical-data-generation-pipeline", request.toAgentContext());
    }
}
