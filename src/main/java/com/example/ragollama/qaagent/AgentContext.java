package com.example.ragollama.qaagent;

import java.util.Map;

/**
 * Контекстный объект-контейнер, передающий данные для работы QA-агента.
 * <p>
 * Этот record является универсальной структурой для передачи любых данных,
 * необходимых для выполнения задачи. Использование {@code Map<String, Object>}
 * обеспечивает максимальную гибкость.
 *
 * @param payload Карта, содержащая входные данные. Ключи должны быть
 *                заранее определены и документированы для каждого агента.
 *                Примеры ключей: "bugReportText", "gitDiff", "prUrl", "logContent".
 */
public record AgentContext(
        Map<String, Object> payload
) {
}
