package com.example.ragollama.monitoring;

import com.example.ragollama.monitoring.model.KnowledgeGap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeGapService {
    private final KnowledgeGapRepository repository;

    @Async("applicationTaskExecutor")
    @Transactional
    public void recordGap(String query) {
        log.warn("Обнаружен пробел в знаниях для запроса: '{}'. Запись сохраняется.", query);
        KnowledgeGap gap = KnowledgeGap.builder()
                .queryText(query)
                .build();
        repository.save(gap);
    }
}
