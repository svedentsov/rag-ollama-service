package com.example.ragollama.optimization.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для структурированного вывода от DocumentEnhancerAgent.
 * Содержит обогащенные метаданные для документа.
 */
@Schema(description = "Обогащенные метаданные для документа")
public record EnhancedMetadata(
        @Schema(description = "Краткое саммари всего документа, сгенерированное AI")
        String summary,

        @Schema(description = "Список ключевых слов или тегов, извлеченных AI")
        List<String> keywords
) {
}
