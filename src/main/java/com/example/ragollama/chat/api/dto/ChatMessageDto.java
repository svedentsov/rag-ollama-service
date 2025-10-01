package com.example.ragollama.chat.api.dto;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.MessageRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO для представления одного сообщения в чате.
 * Включает parentId для построения древовидной структуры на клиенте.
 *
 * @param id       ID сообщения.
 * @param parentId ID родительского сообщения (для ответов ассистента).
 * @param role     Роль отправителя.
 * @param content  Текст сообщения.
 * @param taskId   ID асинхронной задачи, сгенерировавшей это сообщение.
 */
@Schema(description = "DTO для одного сообщения в чате")
public record ChatMessageDto(
        UUID id,
        UUID parentId,
        MessageRole role,
        String content,
        UUID taskId
) {
    /**
     * Фабричный метод для преобразования сущности ChatMessage в DTO.
     *
     * @param entity Сущность для преобразования.
     * @return Новый экземпляр DTO.
     */
    public static ChatMessageDto fromEntity(ChatMessage entity) {
        return new ChatMessageDto(
                entity.getId(),
                entity.getParentId(),
                entity.getRole(),
                entity.getContent(),
                entity.getTaskId()
        );
    }
}
