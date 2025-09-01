package com.example.ragollama.agent.review;

import com.example.ragollama.agent.dynamic.DynamicPipelineExecutionService;
import com.example.ragollama.agent.dynamic.ExecutionState;
import com.example.ragollama.agent.dynamic.ExecutionStateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Сервис для обработки решений человека-рецензента.
 * <p>
 * Инкапсулирует логику изменения состояния конвейера (`ExecutionState`)
 * в ответ на действия пользователя через `ReviewController`.
 */
@Slf4j
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
        ExecutionState state = findAndValidateState(executionId);
        log.info("Выполнение {} утверждено пользователем. Возобновление...", executionId);
        state.setResumedAfterApproval(true);
        executionStateRepository.save(state);
        executionService.resumeExecution(executionId);
    }

    /**
     * Обрабатывает отклонение конвейера.
     *
     * @param executionId ID конвейера.
     */
    @Transactional
    public void reject(UUID executionId) {
        ExecutionState state = findAndValidateState(executionId);

        log.warn("Выполнение {} отклонено пользователем. Завершение работы.", executionId);
        state.setStatus(ExecutionState.Status.FAILED);
        executionStateRepository.save(state);
    }

    /**
     * Находит и валидирует состояние конвейера перед изменением.
     *
     * @param executionId ID конвейера.
     * @return Найденный и валидный {@link ExecutionState}.
     */
    private ExecutionState findAndValidateState(UUID executionId) {
        ExecutionState state = executionStateRepository.findById(executionId)
                .orElseThrow(() -> new EntityNotFoundException("Выполнение с ID " + executionId + " не найдено."));

        if (state.getStatus() != ExecutionState.Status.PENDING_APPROVAL) {
            throw new IllegalStateException("Выполнение " + executionId + " не ожидает утверждения. Текущий статус: " + state.getStatus());
        }
        return state;
    }
}
