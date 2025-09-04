package com.example.ragollama.agent.knowledgegraph.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * DTO для хранения информации о последнем коммите, затронувшем фрагмент кода.
 * <p>Эта информация извлекается с помощью команды `git blame` и предоставляет
 * критически важный контекст для трассировки изменений и отладки.
 *
 * @param commitHash    Короткий хэш коммита.
 * @param authorName    Имя автора.
 * @param authorEmail   Email автора.
 * @param commitTime    Время коммита.
 * @param commitMessage Краткое сообщение коммита.
 */
@Schema(description = "Информация о последнем коммите")
public record LastCommitInfo(
        String commitHash,
        String authorName,
        String authorEmail,
        Instant commitTime,
        String commitMessage
) {
}
