package com.example.ragollama.chat.api.dto;

import com.example.ragollama.chat.domain.model.ChatMessage;
import com.example.ragollama.chat.domain.model.MessageRole;
import com.example.ragollama.evaluation.model.ValidationReport;
import com.example.ragollama.monitoring.model.RagAuditLog;
import com.example.ragollama.optimization.model.TrustScoreReport;
import com.example.ragollama.rag.domain.model.QueryFormationStep;
import com.example.ragollama.rag.domain.model.SourceCitation;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO для представления одного сообщения в чате, обогащенного RAG-контекстом.
 *
 * @param id                    ID сообщения.
 * @param parentId              ID родительского сообщения.
 * @param role                  Роль отправителя.
 * @param content               Текст сообщения.
 * @param createdAt             Время создания.
 * @param taskId                ID асинхронной задачи, сгенерировавшей это сообщение.
 * @param sourceCitations       (Опционально) Источники для RAG-ответа.
 * @param queryFormationHistory (Опционально) История трансформации запроса.
 * @param finalPrompt           (Опционально) Финальный промпт, отправленный в LLM.
 * @param trustScoreReport      (Опционально) Отчет об оценке доверия к ответу.
 * @param validationReport      (Опционально) Отчет от AI-критика о качестве ответа.
 */
@Schema(description = "DTO для одного сообщения в чате с полным RAG-контекстом")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessageDto(
        UUID id,
        UUID parentId,
        MessageRole role,
        String content,
        OffsetDateTime createdAt,
        UUID taskId,
        List<SourceCitation> sourceCitations,
        List<QueryFormationStep> queryFormationHistory,
        String finalPrompt,
        TrustScoreReport trustScoreReport,
        ValidationReport validationReport
) {
    /**
     * Фабричный метод для преобразования базовой сущности ChatMessage в DTO.
     *
     * @param entity Сущность для преобразования.
     * @return Новый экземпляр DTO без RAG-контекста.
     */
    public static ChatMessageDto fromEntity(ChatMessage entity) {
        return new ChatMessageDto(
                entity.getId(),
                entity.getParentId(),
                entity.getRole(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getTaskId(),
                null, null, null, null, null
        );
    }

    /**
     * Фабричный метод для создания обогащенного DTO из сообщения и аудиторской записи.
     *
     * @param message  Сущность сообщения.
     * @param auditLog Запись из аудиторского журнала, содержащая все метаданные.
     * @return Новый экземпляр DTO с полным RAG-контекстом.
     */
    public static ChatMessageDto fromEntityWithAudit(ChatMessage message, RagAuditLog auditLog) {
        return new ChatMessageDto(
                message.getId(),
                message.getParentId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt(),
                message.getTaskId(),
                auditLog.getSourceCitations(),
                auditLog.getQueryFormationHistory(),
                auditLog.getFinalPrompt(),
                null,
                null
        );
    }
}
