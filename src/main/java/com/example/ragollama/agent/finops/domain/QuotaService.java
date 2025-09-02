package com.example.ragollama.agent.finops.domain;

import com.example.ragollama.agent.config.QuotaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Сервис для проверки и управления квотами на использование LLM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final QuotaProperties quotaProperties;
    private final LlmUsageLogRepository usageLogRepository;
    // TODO: В реальной системе здесь была бы зависимость от UserService для получения тарифного плана пользователя
    // private final UserService userService;

    /**
     * Проверяет, не превысит ли пользователь свою квоту с учетом предстоящего расхода.
     *
     * @param username        Имя пользователя.
     * @param tokensToConsume Количество токенов, которое планируется потратить.
     * @return {@code true}, если квота будет превышена, иначе {@code false}.
     */
    @Transactional(readOnly = true)
    public boolean isQuotaExceeded(String username, long tokensToConsume) {
        if (!quotaProperties.enabled()) {
            return false; // Если система квот отключена, ничего не проверяем
        }

        // String userTier = userService.getTier(username); // Реальная логика
        String userTier = getUserTierMock(username); // Мок
        QuotaProperties.Tier tierConfig = quotaProperties.tiers().get(userTier);

        if (tierConfig == null) {
            log.error("Не найдена конфигурация для тарифного плана '{}'. Квота будет отклонена.", userTier);
            return true;
        }

        OffsetDateTime startOfMonth = OffsetDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0).withNano(0);
        long currentUsage = usageLogRepository.sumTotalTokensByUsernameSince(username, startOfMonth).orElse(0L);

        boolean exceeded = (currentUsage + tokensToConsume) > tierConfig.totalTokensLimit();
        if (exceeded) {
            log.warn("Пользователь '{}' превысил квоту. Использовано: {}, Лимит: {}.",
                    username, currentUsage, tierConfig.totalTokensLimit());
        }

        return exceeded;
    }

    /**
     * Мок для получения тарифного плана пользователя. В реальной системе
     * эти данные должны приходить из базы данных пользователей.
     */
    private String getUserTierMock(String username) {
        if ("admin".equals(username)) {
            return "pro";
        }
        return quotaProperties.defaultTier();
    }
}
