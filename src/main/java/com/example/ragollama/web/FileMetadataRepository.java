package com.example.ragollama.web;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Реактивный репозиторий для доступа к метаданным файлов.
 */
@Repository
public interface FileMetadataRepository extends ReactiveCrudRepository<FileMetadata, UUID> {
    /**
     * Находит все файлы для указанного пользователя, отсортированные по дате создания.
     *
     * @param userName Имя пользователя.
     * @return Поток метаданных файлов.
     */
    Flux<FileMetadata> findByUserNameOrderByCreatedAtDesc(String userName);
}
