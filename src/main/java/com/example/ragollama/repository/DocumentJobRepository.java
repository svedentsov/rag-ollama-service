package com.example.ragollama.repository;

import com.example.ragollama.entity.DocumentJob;
import com.example.ragollama.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentJobRepository extends JpaRepository<DocumentJob, UUID> {
    Optional<DocumentJob> findFirstByStatusOrderByCreatedAt(JobStatus status);
}
