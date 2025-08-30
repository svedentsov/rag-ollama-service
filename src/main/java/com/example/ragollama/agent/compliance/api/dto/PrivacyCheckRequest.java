package com.example.ragollama.agent.compliance.api.dto;

import com.example.ragollama.agent.AgentContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.Length;

import java.util.List;
import java.util.Map;

/**
 * DTO для запроса на проверку соответствия политикам конфиденциальности.
 *
 * @param privacyPolicy Политика конфиденциальности компании на естественном языке.
 * @param changedFiles  Список путей к измененным файлам для анализа.
 */
@Schema(description = "DTO для запроса на проверку соответствия политикам конфиденциальности")
public record PrivacyCheckRequest(
        @Schema(description = "Политика конфиденциальности компании", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Запрещено логировать любые PII (email, имя, телефон). Разрешено передавать email во внешнюю систему 'MailService' только по HTTPS.")
        @NotEmpty @Length(min = 50)
        String privacyPolicy,

        @Schema(description = "Список путей к измененным файлам для анализа", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty
        List<String> changedFiles
) {
    /**
     * Преобразует DTO в {@link AgentContext} для передачи в конвейер.
     *
     * @return Контекст для запуска агента.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(Map.of(
                "privacyPolicy", this.privacyPolicy,
                "changedFiles", this.changedFiles
        ));
    }
}
