package com.example.ragollama.qaagent.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для запроса на генерацию отчета о тестовом техническом долге.
 * На данный момент не содержит параметров, но может быть расширен в будущем.
 */
@Schema(description = "DTO для запроса на отчет о тестовом техдолге")
public record TestDebtReportRequest() {
}
