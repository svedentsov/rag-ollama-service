package com.example.ragollama.agent.metrics.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для запроса на сбор метрик тестового прогона из CI/CD.
 * <p>
 * Этот объект является "контрактом" для внешних систем, которые хотят
 * отправлять данные о результатах тестов в нашу аналитическую платформу.
 * Он включает как сам отчет, так и необходимый контекст (проект, коммит).
 *
 * @param projectId       Уникальный идентификатор проекта, из которого пришли метрики.
 *                        Ключевое поле для поддержки федеративного (multi-tenant) анализа.
 * @param junitXmlContent Содержимое JUnit XML отчета в виде строки.
 * @param commitHash      SHA-хэш коммита, для которого выполнялся прогон.
 * @param branchName      Имя ветки, для которой выполнялся прогон.
 */
@Schema(description = "DTO для отправки результатов тестового прогона из CI/CD")
public record TestMetricsCollectionRequest(
        @Schema(description = "Уникальный ID проекта, из которого пришли метрики", requiredMode = Schema.RequiredMode.REQUIRED, example = "phoenix-backend")
        @NotBlank
        String projectId,

        @Schema(description = "Содержимое JUnit XML отчета в виде строки", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String junitXmlContent,

        @Schema(description = "SHA-хэш коммита", requiredMode = Schema.RequiredMode.REQUIRED, example = "a1b2c3d4e5f6")
        @NotBlank @Size(max = 40)
        String commitHash,

        @Schema(description = "Имя ветки", example = "feature/new-logic")
        @Size(max = 255)
        String branchName
) {
}
