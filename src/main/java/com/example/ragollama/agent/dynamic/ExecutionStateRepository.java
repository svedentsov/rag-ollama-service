package com.example.ragollama.agent.dynamic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExecutionStateRepository extends JpaRepository<ExecutionState, UUID> {
}
