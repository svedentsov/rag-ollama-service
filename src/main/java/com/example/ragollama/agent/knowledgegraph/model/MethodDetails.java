package com.example.ragollama.agent.knowledgegraph.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO, содержащий детальную информацию об одном методе в классе.
 * <p>Включает в себя как структурную информацию из AST (имя, строки),
 * так и историческую информацию из Git (последний коммит).
 *
 * @param methodName     Имя метода.
 * @param startLine      Номер строки, где начинается объявление метода.
 * @param endLine        Номер строки, где заканчивается тело метода.
 * @param lastCommitInfo Информация о последнем коммите, затронувшем этот метод.
 */
@Schema(description = "Детальная информация о методе и его истории изменений")
public record MethodDetails(
        String methodName,
        int startLine,
        int endLine,
        LastCommitInfo lastCommitInfo
) {
}
