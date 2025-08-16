package com.example.ragollama.repository;

import com.example.ragollama.entity.DocumentJob;
import com.example.ragollama.entity.JobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link DocumentJob}.
 * Содержит методы для пакетного поиска и обновления задач.
 */
@Repository
public interface DocumentJobRepository extends JpaRepository<DocumentJob, UUID> {

    /**
     * Находит список задач с указанным статусом, отсортированных по дате создания.
     *
     * @param status   Статус для поиска.
     * @param pageable Объект пагинации для ограничения количества результатов (размер пакета).
     * @return Список задач.
     */
    List<DocumentJob> findByStatusOrderByCreatedAt(JobStatus status, Pageable pageable);

    /**
     * Пакетно обновляет статус для списка задач по их идентификаторам.
     * Использование {@code @Modifying} и кастомного HQL/JPQL запроса позволяет
     * выполнить операцию одним SQL-выражением, что значительно эффективнее,
     * чем загрузка и сохранение каждой сущности по отдельности.
     *
     * @param ids    Список UUID задач для обновления.
     * @param status Новый статус, который нужно установить.
     * @return Количество обновленных записей.
     */
    @Modifying
    @Query("UPDATE DocumentJob j SET j.status = :status WHERE j.id IN :ids")
    int updateStatusForIds(@Param("ids") List<UUID> ids, @Param("status") JobStatus status);
}
