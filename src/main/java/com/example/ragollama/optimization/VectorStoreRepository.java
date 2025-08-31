package com.example.ragollama.optimization;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Репозиторий для выполнения низкоуровневых операций с таблицей `vector_store`,
 * которые не покрываются стандартным интерфейсом {@link org.springframework.ai.vectorstore.VectorStore}.
 * <p>
 * Этот компонент инкапсулирует прямой доступ к базе данных для выполнения
 * специфичных для обслуживания задач, таких как удаление по метаданным.
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
     * Использование оператора `->>` позволяет эффективно запрашивать
     * текстовые значения из JSON-поля.
     *
     * @param documentId UUID задачи (`DocumentJob`), чанки которой нужно удалить.
     * @return Количество удаленных строк (чанков).
     */
    public int deleteByDocumentId(UUID documentId) {
        final String sql = "DELETE FROM vector_store WHERE metadata ->> 'documentId' = ?";
        return jdbcTemplate.update(sql, documentId.toString());
    }
}
