package com.example.ragollama.ingestion.domain;

import com.example.ragollama.ingestion.domain.model.DocumentJob;
import com.example.ragollama.ingestion.domain.model.JobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Реактивный репозиторий для DocumentJob.
 */
@Repository
public interface DocumentJobRepository extends ReactiveCrudRepository<DocumentJob, UUID> {

    Flux<DocumentJob> findByStatusOrderByCreatedAt(JobStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE document_jobs SET status = :status WHERE id IN (:ids)")
    Mono<Integer> updateStatusForIds(@Param("ids") List<UUID> ids, @Param("status") JobStatus status);

    @Query("SELECT j.id FROM document_jobs j WHERE j.status = 'COMPLETED' AND j.updated_at < :threshold")
    Flux<UUID> findCompletedJobsBefore(@Param("threshold") OffsetDateTime threshold);
}
