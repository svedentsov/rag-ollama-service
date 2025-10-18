package com.example.ragollama.web;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.UUID;

/**
 * Реактивный репозиторий для доступа к метаданным файлов.
 */
@Repository
public interface FileMetadataRepository extends ReactiveCrudRepository<FileMetadata, UUID> {

    /**
     * Находит метаданные файла по имени пользователя и имени файла.
     *
     * @param userName Имя пользователя.
     * @param fileName Имя файла.
     * @return Mono с найденными метаданными или пустой Mono, если файл не найден.
     */
    Mono<FileMetadata> findByUserNameAndFileName(String userName, String fileName);

    /**
     * Подсчитывает общее количество файлов для пользователя с учетом фильтрации.
     *
     * @param userName Имя пользователя.
     * @param query    Поисковая строка.
     * @return Mono с общим количеством файлов.
     */
    @Query("SELECT COUNT(*) FROM file_metadata WHERE user_name = :userName AND file_name ILIKE :query")
    Mono<Long> countByUserNameWithFilter(String userName, String query);

    /**
     * Находит все файлы для указанного пользователя, отсортированные по дате создания.
     *
     * @param userName Имя пользователя.
     * @return Поток метаданных файлов.
     */
    Flux<FileMetadata> findByUserNameOrderByCreatedAtDesc(String userName);

    /**
     * Удаляет все файлы по списку их ID.
     *
     * @param ids Коллекция ID файлов для удаления.
     * @return Mono, завершающийся после удаления.
     */
    Mono<Void> deleteAllByIdIn(Collection<UUID> ids);
}
