package com.example.ragollama.agent.finops.domain;

import com.example.ragollama.agent.config.QuotaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Сервис для проверки и управления квотами на использование LLM, адаптированный для R2DBC.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final QuotaProperties quotaProperties;
    private final DatabaseClient databaseClient;

    /**
     * Проверяет, не превысит ли пользователь свою квоту на использование токенов.
     *
     * @param username        Имя пользователя.
     * @param tokensToConsume Количество токенов, которое планируется использовать.
     * @return {@link Mono<Boolean>}, который эммитит `true`, если квота будет превышена.
     */
    @Transactional(readOnly = true)
    public Mono<Boolean> isQuotaExceeded(String username, long tokensToConsume) {
        if (!quotaProperties.enabled()) {
            return Mono.just(false);
        }

        String userTier = getUserTierMock(username);
        QuotaProperties.Tier tierConfig = quotaProperties.tiers().get(userTier);

        if (tierConfig == null) {
            log.error("Не найдена конфигурация для тарифного плана '{}'. Квота будет отклонена.", userTier);
            return Mono.just(true);
        }

        OffsetDateTime startOfMonth = OffsetDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0).withNano(0);

        String sql = "SELECT COALESCE(SUM(l.total_tokens), 0) FROM llm_usage_log l WHERE l.username = :username AND l.created_at >= :since";

        return databaseClient.sql(sql)
                .bind("username", username)
                .bind("since", startOfMonth)
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L)
                .map(currentUsage -> {
                    boolean exceeded = (currentUsage + tokensToConsume) > tierConfig.totalTokensLimit();
                    if (exceeded) {
                        log.warn("Пользователь '{}' превысил квоту. Использовано: {}, Лимит: {}.",
                                username, currentUsage, tierConfig.totalTokensLimit());
                    }
                    return exceeded;
                });
    }

    private String getUserTierMock(String username) {
        if ("admin".equals(username)) {
            return "pro";
        }
        return quotaProperties.defaultTier();
    }
}
