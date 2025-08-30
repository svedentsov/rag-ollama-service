package com.example.ragollama.agent;

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
    /**
     * Перечисление возможных статусов завершения работы агента.
     */
    public enum Status {
        /**
         * Агент успешно выполнил свою задачу.
         */
        SUCCESS,
        /**
         * В процессе выполнения агента произошла ошибка.
         */
        FAILURE
    }
}
