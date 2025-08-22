package com.example.ragollama.qaagent;

import java.util.Map;

/**
 * Стандартизированный результат работы QA-агента.
 *
 * @param agentName Имя агента, сгенерировавшего результат.
 * @param status    Статус выполнения (SUCCESS, FAILURE).
 * @param summary   Краткое, человекочитаемое резюме результата.
 * @param details   Карта с детальными данными (например, список найденных
 *                  дубликатов, сгенерированный код, предложенные селекторы).
 */
public record AgentResult(
        String agentName,
        Status status,
        String summary,
        Map<String, Object> details
) {
    public enum Status {
        SUCCESS,
        FAILURE
    }
}
