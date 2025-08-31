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
 * Сервис для обработки решений человека-рецензента в рамках
 * механизма Human-in-the-Loop.
 * <p>
 * Этот сервис является "мостом" между API утверждения и ядром
 * выполнения динамических конвейеров.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ExecutionStateRepository executionStateRepository;
    private final DynamicPipelineExecutionService executionService;

    /**
     * Обрабатывает утверждение приостановленного конвейера.
     * <p>
     * Метод находит конвейер в статусе `PENDING_APPROVAL`, устанавливает
     * флаг `resumedAfterApproval` в `true`, чтобы сигнализировать исполнителю,
     * что проверка `requiresApproval()` для текущего шага может быть пропущена,
     * и затем асинхронно запускает возобновление выполнения.
     *
     * @param executionId ID конвейера, ожидающего утверждения.
     * @throws EntityNotFoundException если конвейер с указанным ID не найден.
     * @throws IllegalStateException   если конвейер не находится в статусе,
     *                                 допускающем утверждение.
     */
    @Transactional
    public void approve(UUID executionId) {
        ExecutionState state = executionStateRepository.findById(executionId)
                .orElseThrow(() -> new EntityNotFoundException("Выполнение с ID " + executionId + " не найдено."));
        if (state.getStatus() != ExecutionState.Status.PENDING_APPROVAL) {
            throw new IllegalStateException("Выполнение " + executionId + " не ожидает утверждения.");
        }
        state.setResumedAfterApproval(true);
        executionStateRepository.save(state);
        executionService.resumeExecution(executionId);
    }
}
