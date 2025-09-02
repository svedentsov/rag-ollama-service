package com.example.ragollama.agent.finops.domain;

import com.example.ragollama.agent.finops.model.LlmUsageLog;
import com.example.ragollama.shared.llm.model.LlmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Асинхронный сервис для записи логов использования LLM в базу данных.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmUsageTracker {

    private final LlmUsageLogRepository usageLogRepository;

    /**
     * Асинхронно сохраняет запись об использовании LLM.
     * Выполняется в отдельном потоке, чтобы не замедлять ответ пользователю.
     *
     * @param modelName Имя использованной модели.
     * @param response  Объект ответа от LLM, содержащий метаданные об использовании.
     */
    @Async("applicationTaskExecutor")
    @Transactional
    public void trackUsage(String modelName, LlmResponse response) {
        Usage usage = response.usage();
        if (usage == null) {
            log.warn("Не удалось записать использование для модели {}, так как метаданные Usage отсутствуют.", modelName);
            return;
        }

        String username = getAuthenticatedUsername();

        LlmUsageLog logEntry = LlmUsageLog.builder()
                .username(username)
                .modelName(modelName)
                .promptTokens((long) usage.getPromptTokens())
                .completionTokens((long) usage.getCompletionTokens())
                .totalTokens((long) usage.getTotalTokens())
                .build();

        usageLogRepository.save(logEntry);
        log.debug("Записано использование LLM для пользователя '{}': {} токенов.", username, logEntry.getTotalTokens());
    }

    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "system_user"; // Fallback для системных вызовов
    }
}
