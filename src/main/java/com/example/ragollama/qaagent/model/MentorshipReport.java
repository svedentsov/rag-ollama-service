package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для финального отчета от Test Mentor Bot.
 *
 * @param overallFeedback  Общий фидбэк и высокоуровневые выводы от AI-наставника.
 * @param detailedFindings Список детальных замечаний и похвал по конкретным аспектам кода.
 * @param suggestedCode    Полностью отрефакторенная, эталонная версия тестового класса.
 */
@Schema(description = "Полный отчет-ревью от AI-наставника по тестированию")
@JsonIgnoreProperties(ignoreUnknown = true)
public record MentorshipReport(
        @Schema(description = "Общий фидбэк и выводы")
        String overallFeedback,
        @Schema(description = "Детальный разбор по аспектам")
        List<MentorshipFinding> detailedFindings,
        @Schema(description = "Предложенная эталонная версия кода")
        String suggestedCode
) {
}
