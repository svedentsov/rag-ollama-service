package com.example.ragollama.monitoring.domain;

import com.example.ragollama.monitoring.api.dto.FeedbackRequest;
import com.example.ragollama.monitoring.model.FeedbackLog;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Сервис-оркестратор для обработки входящей обратной связи.
 * <p>
 * Отвечает за быструю валидацию, сохранение "сырого" фидбэка и
 * запуск асинхронного обработчика для генерации обучающих данных.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackLogRepository feedbackLogRepository;
    private final RagAuditLogRepository auditLogRepository;
    private final FeedbackHandlerService feedbackHandlerService;

    /**
     * Обрабатывает запрос на обратную связь.
     *
     * @param request DTO с данными от пользователя.
     * @throws EntityNotFoundException если RAG-взаимодействие с указанным `requestId` не найдено.
     */
    @Transactional
    public void processFeedback(FeedbackRequest request) {
        // Проверяем, что исходный запрос существует в аудите
        if (!auditLogRepository.existsByRequestId(request.requestId())) {
            throw new EntityNotFoundException("RAG interaction with request ID " + request.requestId() + " not found.");
        }

        FeedbackLog feedbackLog = FeedbackLog.builder()
                .requestId(request.requestId())
                .isHelpful(request.isHelpful())
                .userComment(request.comment())
                .build();
        FeedbackLog savedFeedback = feedbackLogRepository.save(feedbackLog);

        // Запускаем асинхронную тяжелую обработку только после успешного коммита транзакции
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("Фидбэк для requestId {} сохранен. Запуск асинхронного обработчика...", request.requestId());
                feedbackHandlerService.generateTrainingDataFromFeedback(savedFeedback);
            }
        });
    }
}
