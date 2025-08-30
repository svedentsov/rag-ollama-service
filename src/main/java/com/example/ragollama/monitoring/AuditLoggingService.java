package com.example.ragollama.monitoring;

import com.example.ragollama.monitoring.domain.RagAuditLogRepository;
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
import java.util.concurrent.CompletableFuture;

/**
 * Асинхронный сервис для записи аудиторских логов RAG-взаимодействий.
 * <p>
 * Обеспечивает персистентное сохранение полного контекста каждого запроса
 * (включая промпт, контекст, ответ) для целей отладки, анализа и комплаенса.
 * Эта версия явно принимает `requestId` для надежной трассировки.
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
     * ответа основному пользователю. Возвращает {@link CompletableFuture} для
     * возможности отслеживания завершения операции.
     *
     * @param requestId        Уникальный идентификатор HTTP-запроса, переданный явно.
     * @param sessionId        Идентификатор сессии диалога.
     * @param originalQuery    Исходный запрос пользователя.
     * @param contextDocuments Список источников, использованных в контексте.
     * @param finalPrompt      Финальный промпт, отправленный в LLM.
     * @param llmAnswer        Ответ, сгенерированный LLM.
     * @return {@link CompletableFuture}, который завершается после сохранения.
     */
    @Async("applicationTaskExecutor")
    @Transactional
    public CompletableFuture<Void> logInteractionAsync(
            String requestId,
            UUID sessionId,
            String originalQuery,
            List<String> contextDocuments,
            String finalPrompt,
            String llmAnswer) {
        try {
            String username = "SYSTEM";
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
        return CompletableFuture.completedFuture(null);
    }
}
