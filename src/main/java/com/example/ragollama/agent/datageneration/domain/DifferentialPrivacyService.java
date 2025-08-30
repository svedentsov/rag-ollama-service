package com.example.ragollama.agent.datageneration.domain;

import com.example.ragollama.agent.datageneration.model.DataProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Сервис для применения техник дифференциальной приватности к статистическому профилю.
 * <p>
 * ВАЖНО: Эта реализация является **mock-заглушкой** для демонстрации.
 * В реальном проекте здесь будет интеграция с полноценной DP-библиотекой
 * (например, Google's DP Library, OpenDP) для применения математически строгих
 * механизмов (например, механизм Лапласа для добавления шума).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DifferentialPrivacyService {

    /**
     * Применяет "зашумление" к статистическому профилю для обеспечения приватности.
     *
     * @param profile Исходный статистический профиль.
     * @param epsilon Параметр приватности (бюджет).
     * @return Новый, "приватный" статистический профиль.
     */
    public DataProfile privatize(DataProfile profile, double epsilon) {
        log.info("Применение дифференциальной приватности к профилю с epsilon={}", epsilon);

        // Mock-логика: просто немного изменяем счетчики, чтобы симулировать шум.
        Map<String, DataProfile.ColumnProfile> privateColumnProfiles = new HashMap<>();
        profile.getColumnProfiles().forEach((name, colProfile) -> {
            Map<String, Long> noisyCounts = new HashMap<>();
            if (colProfile.getValueCounts() != null) {
                colProfile.getValueCounts().forEach((value, count) -> {
                    // Симулируем добавление шума Лапласа
                    long noise = (long) (Math.random() * (1 / epsilon) * 2 - (1 / epsilon));
                    noisyCounts.put(value, Math.max(0, count + noise));
                });
            }

            DataProfile.ColumnProfile privateColProfile = DataProfile.ColumnProfile.builder()
                    .dataType(colProfile.getDataType())
                    .valueCounts(noisyCounts)
                    .nullCount(Math.max(0, colProfile.getNullCount() + (long) (Math.random() * 2 - 1)))
                    .mean(colProfile.getMean() != null ? colProfile.getMean() + (Math.random() * 0.1 - 0.05) : null)
                    .build();
            privateColumnProfiles.put(name, privateColProfile);
        });

        return DataProfile.builder()
                .rowCount(profile.getRowCount())
                .columnProfiles(privateColumnProfiles)
                .build();
    }
}
