package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * DTO для представления одной команды, сгенерированной LLM-агентом.
 *
 * @param command   Название команды (click, fill, goTo, assert).
 * @param arguments Карта с аргументами для команды.
 * @param thought   Объяснение от AI, почему он выбрал это действие.
 */
@Schema(description = "Одна команда, сгенерированная AI-агентом")
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentCommand(
        String command,
        Map<String, Object> arguments,
        String thought
) {
}
