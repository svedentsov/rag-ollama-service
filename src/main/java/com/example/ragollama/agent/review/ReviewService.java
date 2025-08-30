package com.example.ragollama.agent.review;

import com.example.ragollama.agent.dynamic.DynamicPipelineExecutionService;
import com.example.ragollama.agent.dynamic.ExecutionState;
import com.example.ragollama.agent.dynamic.ExecutionStateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Сервис для обработки решений человека-рецензента.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ExecutionStateRepository executionStateRepository;
    private final DynamicPipelineExecutionService executionService;

    /**
     * Обрабатывает утверждение конвейера.
     *
     * @param executionId ID конвейера.
     */
    @Transactional
    public void approve(UUID executionId) {
        ExecutionState state = executionStateRepository.findById(executionId)
                .orElseThrow(() -> new EntityNotFoundException("Execution with ID " + executionId + " not found."));

        if (state.getStatus() != ExecutionState.Status.PENDING_APPROVAL) {
            throw new IllegalStateException("Execution " + executionId + " is not awaiting approval.");
        }

        // Помечаем, что можно продолжить
        state.setStatus(ExecutionState.Status.RESUMED_AFTER_APPROVAL);
        executionStateRepository.save(state);

        // Асинхронно запускаем продолжение
        executionService.resumeExecution(executionId);
    }
}
