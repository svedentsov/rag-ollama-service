package com.example.ragollama.optimization;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
     * @param documentId Уникальный идентификатор документа, чанки которого нужно удалить.
     * @return Количество удаленных строк (чанков).
     */
    public int deleteByDocumentId(String documentId) {
        final String sql = "DELETE FROM vector_store WHERE metadata ->> 'documentId' = ?";
        return jdbcTemplate.update(sql, documentId);
    }
}
