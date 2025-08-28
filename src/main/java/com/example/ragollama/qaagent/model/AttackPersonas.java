package com.example.ragollama.qaagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO для представления набора "персон", сгенерированных для тестирования RBAC.
 *
 * @param attackScenario Описание сценария атаки, который эти персоны реализуют.
 * @param personas       Список сгенерированных персон.
 */
@Schema(description = "Набор персон для симуляции атаки на RBAC")
@JsonIgnoreProperties(ignoreUnknown = true)
public record AttackPersonas(
        @Schema(description = "Описание сценария атаки")
        String attackScenario,
        @Schema(description = "Список персон")
        List<Persona> personas
) {
    /**
     * DTO для одной персоны.
     *
     * @param role        Роль (например, "ROLE_USER").
     * @param tenantId    Идентификатор тенанта (например, "tenant-a").
     * @param description Описание роли персоны в сценарии (Attacker, Victim).
     */
    @Schema(description = "Одна персона для тестирования")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Persona(String role, String tenantId, String description) {
    }
}
