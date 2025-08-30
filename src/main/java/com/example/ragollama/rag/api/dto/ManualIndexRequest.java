package com.example.ragollama.rag.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

/**
 * DTO для запроса на ручную, on-demand индексацию одного тест-кейса.
 *
 * @param filePath Уникальный идентификатор тест-кейса (полный путь к файлу).
 * @param content  Полное содержимое файла с исходным кодом теста.
 */
@Schema(description = "DTO для запроса на ручную индексацию тест-кейса")
public record ManualIndexRequest(
        @Schema(description = "Уникальный ID/путь к файлу теста", requiredMode = Schema.RequiredMode.REQUIRED, example = "src/test/java/com/example/UserServiceTest.java")
        @NotBlank
        String filePath,

        @Schema(description = "Полное содержимое файла теста", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Length(min = 50)
        String content
) {
}
