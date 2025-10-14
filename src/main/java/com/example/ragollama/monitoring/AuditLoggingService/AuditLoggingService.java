package com.example.ragollama.monitoring.AuditLoggingService;

import com.example.ragollama.monitoring.domain.RagAuditLogRepository;
import com.example.ragollama.monitoring.model.RagAuditLog;
import com.example.ragollama.rag.domain.model.QueryFormationStep;
import com.example.ragollama.rag.domain.model.SourceCitation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для асинхронной записи аудиторских логов RAG-взаимодействий.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLoggingService {

    private final RagAuditLogRepository auditLogRepository;

    /**
     * Асинхронно сохраняет полную запись о RAG-взаимодействии в новой транзакции.
     *
     * @param requestId             Уникальный идентификатор HTTP-запроса.
     * @param taskId                ID асинхронной задачи.
     * @param sessionId             Идентификатор сессии диалога.
     * @param originalQuery         Исходный запрос пользователя.
     * @param sourceCitations       Список структурированных цитат.
     * @param finalPrompt           Финальный промпт, отправленный в LLM.
     * @param llmAnswer             Ответ, сгенерированный LLM.
     * @param queryFormationHistory История трансформации запроса.
     * @return {@link Mono<Void>}, который завершается после успешного сохранения.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<Void> logInteraction(
            String requestId,
            UUID taskId,
            UUID sessionId,
            String originalQuery,
            List<SourceCitation> sourceCitations,
            String finalPrompt,
            String llmAnswer,
            List<QueryFormationStep> queryFormationHistory
    ) {
        String username = "anonymous"; // Заглушка, пока нет аутентификации

        RagAuditLog auditLog = RagAuditLog.builder()
                .requestId(requestId)
                .taskId(taskId)
                .sessionId(sessionId)
                .username(username)
                .originalQuery(originalQuery)
                .sourceCitations(sourceCitations)
                .finalPrompt(finalPrompt)
                .llmAnswer(llmAnswer)
                .queryFormationHistory(queryFormationHistory)
                .build();

        return auditLogRepository.save(auditLog)
                .doOnSuccess(saved -> log.debug("Аудиторская запись для requestId {} успешно сохранена.", requestId))
                .doOnError(e -> log.error("Не удалось сохранить аудиторскую запись для requestId {}: {}", requestId, e.getMessage(), e))
                .then();
    }
}
