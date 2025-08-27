package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для структурированного результата рефакторинга кода автотеста.
 * <p>
 * Этот объект является "продуктом" работы агента {@link com.example.ragollama.qaagent.impl.TestSmellRefactorerAgent}.
 *
 * @param smellsFound    Список "запахов" (плохих практик), обнаруженных в исходном коде.
 * @param justification  Объяснение от LLM, почему предложенный рефакторинг улучшает код.
 * @param refactoredCode Полный, готовый к использованию, улучшенный код теста.
 */
@Schema(description = "Результат рефакторинга кода автотеста")
@JsonIgnoreProperties(ignoreUnknown = true)
public record TestRefactoringResult(
        @Schema(description = "Список 'запахов', найденных в исходном коде")
        List<String> smellsFound,
        @Schema(description = "Объяснение, почему рефакторинг улучшает код")
        String justification,
        @Schema(description = "Предложенный улучшенный код теста")
        String refactoredCode
) {
}
