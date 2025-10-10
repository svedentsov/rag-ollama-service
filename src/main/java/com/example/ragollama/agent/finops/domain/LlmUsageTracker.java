package com.example.ragollama.agent.finops.domain;

import com.example.ragollama.agent.finops.model.LlmUsageLog;
import com.example.ragollama.shared.llm.model.LlmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Асинхронный сервис для записи логов использования LLM в базу данных.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmUsageTracker {

    private final LlmUsageLogRepository usageLogRepository;

    /**
     * Асинхронно сохраняет запись об использовании LLM в новой транзакции.
     *
     * @param modelName Имя использованной модели.
     * @param response  Объект ответа от LLM, содержащий метаданные об использовании.
     * @return {@link Mono<Void>}, который завершается после успешного сохранения.
     *         Для запуска операции необходимо подписаться на этот Mono.
     */
    @Transactional
    public Mono<Void> trackUsage(String modelName, LlmResponse response) {
        Usage usage = response.usage();
        if (usage == null) {
            log.warn("Не удалось записать использование для модели {}, так как метаданные Usage отсутствуют.", modelName);
            return Mono.empty();
        }
        String username = getAuthenticatedUsername();
        LlmUsageLog logEntry = LlmUsageLog.builder()
                .username(username)
                .modelName(modelName)
                .promptTokens((long) usage.getPromptTokens())
                .completionTokens((long) usage.getCompletionTokens())
                .totalTokens((long) usage.getTotalTokens())
                .build();

        return usageLogRepository.save(logEntry)
                .doOnSuccess(saved -> log.debug("Записано использование LLM для пользователя '{}': {} токенов.", username, saved.getTotalTokens()))
                .then();
    }

    /**
     * Возвращает имя пользователя для логирования.
     * Поскольку аутентификация удалена из проекта, все действия
     * считаются анонимными.
     *
     * @return Статическое имя "anonymous_user".
     */
    private String getAuthenticatedUsername() {
        return "anonymous_user";
    }
}
