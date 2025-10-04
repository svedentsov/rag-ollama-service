package com.example.ragollama.monitoring.AuditLoggingService;

import com.example.ragollama.monitoring.domain.RagAuditLogRepository;
import com.example.ragollama.monitoring.model.RagAuditLog;
import com.example.ragollama.rag.domain.model.SourceCitation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для записи аудиторских логов RAG-взаимодействий.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLoggingService {

    private final RagAuditLogRepository auditLogRepository;

    /**
     * Синхронно сохраняет полную запись о RAG-взаимодействии в базу данных.
     *
     * @param requestId       Уникальный идентификатор HTTP-запроса.
     * @param sessionId       Идентификатор сессии диалога.
     * @param originalQuery   Исходный запрос пользователя.
     * @param sourceCitations Список структурированных цитат.
     * @param finalPrompt     Финальный промпт, отправленный в LLM.
     * @param llmAnswer       Ответ, сгенерированный LLM.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logInteraction(
            String requestId,
            UUID sessionId,
            String originalQuery,
            List<SourceCitation> sourceCitations,
            String finalPrompt,
            String llmAnswer) {
        try {
            String username = "anonymous"; // Заглушка, пока нет аутентификации

            RagAuditLog auditLog = RagAuditLog.builder()
                    .requestId(requestId)
                    .sessionId(sessionId)
                    .username(username)
                    .originalQuery(originalQuery)
                    .sourceCitations(sourceCitations)
                    .finalPrompt(finalPrompt)
                    .llmAnswer(llmAnswer)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Аудиторская запись для requestId {} успешно сохранена.", requestId);
        } catch (Exception e) {
            log.error("Не удалось сохранить аудиторскую запись для requestId {}: {}", requestId, e.getMessage(), e);
        }
    }
}
