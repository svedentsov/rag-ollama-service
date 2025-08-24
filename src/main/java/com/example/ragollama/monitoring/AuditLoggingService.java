package com.example.ragollama.monitoring;

import com.example.ragollama.monitoring.model.RagAuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Асинхронный сервис для записи аудиторских логов RAG-взаимодействий.
 * <p>
 * Обеспечивает персистентное сохранение полного контекста каждого запроса
 * (включая промпт, контекст, ответ) для целей отладки, анализа и комплаенса.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLoggingService {

    private final RagAuditLogRepository auditLogRepository;

    /**
     * Асинхронно сохраняет полную запись о RAG-взаимодействии в базу данных.
     * <p>
     * Метод выполняется в отдельном потоке, чтобы не влиять на задержку
     * ответа основному пользователю. Добавлена отказоустойчивая логика
     * для получения имени пользователя.
     *
     * @param requestId        Уникальный идентификатор HTTP-запроса из MDC.
     * @param sessionId        Идентификатор сессии диалога.
     * @param originalQuery    Исходный запрос пользователя.
     * @param contextDocuments Список документов-источников, использованных в контексте.
     * @param finalPrompt      Финальный промпт, отправленный в LLM.
     * @param llmAnswer        Ответ, сгенерированный LLM.
     */
    @Async("applicationTaskExecutor")
    @Transactional
    public void logInteractionAsync(
            String requestId,
            UUID sessionId,
            String originalQuery,
            List<String> contextDocuments,
            String finalPrompt,
            String llmAnswer) {
        try {
            String username = "SYSTEM"; // Значение по умолчанию
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                username = authentication.getName();
            }

            RagAuditLog auditLog = RagAuditLog.builder()
                    .requestId(requestId)
                    .sessionId(sessionId)
                    .username(username)
                    .originalQuery(originalQuery)
                    .contextDocuments(contextDocuments)
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
