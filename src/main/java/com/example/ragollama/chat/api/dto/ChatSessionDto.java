package com.example.ragollama.chat.api.dto;

import com.example.ragollama.chat.domain.model.ChatSession;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO для представления сессии чата.
 *
 * <p>Этот record является частью публичного API и скрывает внутреннюю реализацию
 * хранения данных. Он предоставляет клиенту строго типизированные данные,
 * необходимые для отображения списка чатов и управления ветвлением диалогов.
 *
 * @param sessionId            Идентификатор сессии.
 * @param chatName             Пользовательское название чата.
 * @param lastMessageContent   Содержимое последнего сообщения для превью в списке чатов.
 * @param lastMessageTimestamp Временная метка последнего сообщения.
 * @param activeBranches       Карта, хранящая выбор активных веток диалога.
 *                             Ключ - ID родительского сообщения, значение - ID выбранного дочернего.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO для сессии чата")
public record ChatSessionDto(
        UUID sessionId,
        String chatName,
        String lastMessageContent,
        OffsetDateTime lastMessageTimestamp,
        Map<String, String> activeBranches
) {
    /**
     * Фабричный метод для безопасного преобразования доменной сущности {@link ChatSession} в публичный DTO.
     */
    public static ChatSessionDto fromEntity(ChatSession entity) {
        String content = (entity.getLastMessage() != null) ? entity.getLastMessage().getContent() : null;
        OffsetDateTime timestamp = (entity.getLastMessage() != null) ? entity.getLastMessage().getCreatedAt() : entity.getUpdatedAt();

        // Безопасное преобразование Map<String, Object> в Map<String, String>
        Map<String, String> branches = entity.getActiveBranches().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> String.valueOf(entry.getValue())
                ));

        return new ChatSessionDto(
                entity.getSessionId(),
                entity.getChatName(),
                content,
                timestamp,
                branches
        );
    }

    /**
     * DTO для запроса на обновление имени чата.
     *
     * @param newName Новое имя для сессии чата.
     */
    @Schema(description = "DTO для обновления имени чата")
    public record UpdateRequest(
            @NotBlank @Size(min = 1, max = 255)
            String newName
    ) {
    }

    /**
     * DTO для запроса на установку активной ветки диалога.
     *
     * @param parentMessageId ID родительского сообщения, для которого выбирается ветка.
     * @param activeChildId   ID дочернего сообщения, которое становится активным.
     */
    @Schema(description = "DTO для выбора активной ветки")
    public record UpdateActiveBranchRequest(
            @NotNull UUID parentMessageId,
            @NotNull UUID activeChildId
    ) {
    }
}
