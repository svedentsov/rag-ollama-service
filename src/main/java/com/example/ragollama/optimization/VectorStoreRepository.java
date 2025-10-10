package com.example.ragollama.optimization;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Репозиторий для выполнения низкоуровневых операций с таблицей `vector_store`,
 * которые не покрываются стандартным интерфейсом {@link org.springframework.ai.vectorstore.VectorStore}.
 * Эта версия использует неблокирующий {@link DatabaseClient}.
 */
@Repository
@RequiredArgsConstructor
public class VectorStoreRepository {

    private final DatabaseClient databaseClient;

    /**
     * Асинхронно удаляет все чанки, принадлежащие определенному документу.
     *
     * @param documentId Уникальный идентификатор документа.
     * @return {@link Mono} с количеством удаленных строк.
     */
    public Mono<Long> deleteByDocumentId(String documentId) {
        final String sql = "DELETE FROM vector_store WHERE metadata ->> 'documentId' = :documentId";
        return databaseClient.sql(sql)
                .bind("documentId", documentId)
                .fetch()
                .rowsUpdated();
    }

    /**
     * Асинхронно удаляет все чанки для списка идентификаторов документов.
     *
     * @param documentIds Список ID документов.
     * @return {@link Mono} с количеством удаленных строк.
     */
    public Mono<Long> deleteByDocumentIds(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return Mono.just(0L);
        }
        final String sql = "DELETE FROM vector_store WHERE metadata ->> 'documentId' IN (:documentIds)";
        return databaseClient.sql(sql)
                .bind("documentIds", documentIds)
                .fetch()
                .rowsUpdated();
    }
}
