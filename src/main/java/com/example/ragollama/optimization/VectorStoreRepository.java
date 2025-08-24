package com.example.ragollama.optimization;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Репозиторий для выполнения низкоуровневых операций с таблицей `vector_store`,
 * которые не покрываются стандартным интерфейсом {@link org.springframework.ai.vectorstore.VectorStore}.
 */
@Repository
@RequiredArgsConstructor
public class VectorStoreRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Удаляет все чанки (векторы) из хранилища, которые принадлежат
     * определенному документу-источнику.
     * <p>
     * Поиск осуществляется по полю `documentId` в метаданных JSONB.
     *
     * @param documentId UUID задачи (`DocumentJob`), чанки которой нужно удалить.
     * @return Количество удаленных строк (чанков).
     */
    public int deleteByDocumentId(UUID documentId) {
        final String sql = "DELETE FROM vector_store WHERE metadata ->> 'documentId' = ?";
        return jdbcTemplate.update(sql, documentId.toString());
    }
}
