package com.example.ragollama.agent.analytics.api;

import com.example.ragollama.agent.AgentResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Публичный DTO для представления результата работы одного агента.
 * <p>
 * Этот record является частью антикоррупционного слоя, отделяя
 * публичный API-контракт от внутренней доменной модели {@link AgentResult}.
 *
 * @param agentName Имя агента, сгенерировавшего результат.
 * @param status    Статус выполнения.
 * @param summary   Краткое, человекочитаемое резюме.
 * @param details   Карта с детализированными, структурированными данными.
 */
@Getter
@Builder
@Schema(description = "Стандартизированный ответ от одного выполненного агента")
public class AgentExecutionResponse {
    private final String agentName;
    private final AgentResult.Status status;
    private final String summary;
    private final Map<String, Object> details;
}
