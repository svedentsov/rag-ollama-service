package com.example.ragollama.agent.review;

import com.example.ragollama.agent.dynamic.DynamicPipelineExecutionService;
import com.example.ragollama.agent.dynamic.ExecutionState;
import com.example.ragollama.agent.dynamic.ExecutionStateRepository;
import com.example.ragollama.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Сервис для обработки решений человека-рецензента, адаптированный для R2DBC.
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
     * @return {@link Mono}, завершающийся после выполнения операции.
     */
    @Transactional
    public Mono<Void> approve(UUID executionId) {
        return findAndValidateState(executionId)
                .doOnNext(state -> log.info("Выполнение {} утверждено пользователем. Возобновление...", executionId))
                .flatMap(state -> executionService.resumeExecution(executionId));
    }

    /**
     * Обрабатывает отклонение конвейера.
     *
     * @param executionId ID конвейера.
     * @return {@link Mono}, завершающийся после выполнения операции.
     */
    @Transactional
    public Mono<Void> reject(UUID executionId) {
        return findAndValidateState(executionId)
                .doOnNext(state -> log.warn("Выполнение {} отклонено пользователем. Завершение работы.", executionId))
                .flatMap(state -> {
                    state.setStatus(ExecutionState.Status.FAILED);
                    return executionStateRepository.save(state);
                })
                .then();
    }

    /**
     * Находит и валидирует состояние конвейера перед изменением.
     *
     * @param executionId ID конвейера.
     * @return {@link Mono} с найденным и валидным {@link ExecutionState}.
     */
    private Mono<ExecutionState> findAndValidateState(UUID executionId) {
        return executionStateRepository.findById(executionId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Выполнение с ID " + executionId + " не найдено.")))
                .handle((state, sink) -> {
                    if (state.getStatus() != ExecutionState.Status.PENDING_APPROVAL) {
                        sink.error(new IllegalStateException("Выполнение " + executionId + " не ожидает утверждения. Текущий статус: " + state.getStatus()));
                    } else {
                        sink.next(state);
                    }
                });
    }
}
