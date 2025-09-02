package com.example.ragollama.agent.finops.domain;

import com.example.ragollama.agent.finops.model.LlmUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LlmUsageLogRepository extends JpaRepository<LlmUsageLog, UUID> {

    /**
     * Рассчитывает суммарное количество токенов, использованных пользователем
     * за определенный период (обычно текущий месяц).
     *
     * @param username Имя пользователя.
     * @param since    Начало периода.
     * @return {@link Optional} с суммой токенов или пустой, если использований не было.
     */
    @Query("SELECT SUM(l.totalTokens) FROM LlmUsageLog l WHERE l.username = :username AND l.createdAt >= :since")
    Optional<Long> sumTotalTokensByUsernameSince(@Param("username") String username, @Param("since") OffsetDateTime since);
}
