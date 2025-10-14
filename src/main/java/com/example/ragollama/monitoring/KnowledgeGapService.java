package com.example.ragollama.monitoring;

import com.example.ragollama.monitoring.domain.KnowledgeGapRepository;
import com.example.ragollama.monitoring.model.KnowledgeGap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Сервис для асинхронной записи "пробелов в знаниях" — пользовательских
 * запросов, на которые RAG-система не смогла найти релевантных документов.
 * <p>
 * Логирование таких запросов является критически важной частью MLOps,
 * так как предоставляет данные для целенаправленного улучшения
 * базы знаний.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeGapService {

    private final KnowledgeGapRepository repository;

    /**
     * Асинхронно сохраняет запись о пробеле в знаниях.
     * <p>
     * Этот метод теперь возвращает {@code Mono<Void>} и не использует {@code @Async},
     * что делает его полностью совместимым с реактивными транзакциями.
     *
     * @param query Запрос пользователя, на который не был найден ответ.
     * @return {@link Mono}, завершающийся после сохранения записи.
     */
    @Transactional
    public Mono<Void> recordGap(String query) {
        log.warn("Обнаружен пробел в знаниях для запроса: '{}'. Запись сохраняется.", query);
        KnowledgeGap gap = KnowledgeGap.builder()
                .queryText(query)
                .build();
        return repository.save(gap).then();
    }
}
