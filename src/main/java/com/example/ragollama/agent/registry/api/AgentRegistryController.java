package com.example.ragollama.agent.registry.api;

import com.example.ragollama.agent.registry.ToolRegistryService;
import com.example.ragollama.agent.registry.model.AgentDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * Контроллер для динамического управления реестром агентов-инструментов.
 */
@RestController
@RequestMapping("/api/v1/registry/agents")
@RequiredArgsConstructor
@Tag(name = "Agent Marketplace & Registry", description = "API для динамического управления агентами")
public class AgentRegistryController {

    private final ToolRegistryService toolRegistryService;

    /**
     * Регистрирует новый агент-инструмент в системе "на лету".
     *
     * @param definition DTO с описанием агента.
     * @return {@link ResponseEntity} со статусом 200 OK.
     */
    @PostMapping
    @Operation(summary = "Зарегистрировать новый агент-инструмент")
    public ResponseEntity<Void> registerAgent(@Valid @RequestBody AgentDefinition definition) {
        toolRegistryService.register(definition);
        return ResponseEntity.ok().build();
    }

    /**
     * Возвращает список всех зарегистрированных в данный момент агентов.
     *
     * @return Список определений агентов.
     */
    @GetMapping
    @Operation(summary = "Получить список всех зарегистрированных агентов")
    public Collection<AgentDefinition> listAgents() {
        return toolRegistryService.getAllAgents();
    }

    /**
     * Удаляет агент из реестра.
     *
     * @param agentName Имя агента для удаления.
     * @return {@link ResponseEntity} со статусом 204 No Content.
     */
    @DeleteMapping("/{agentName}")
    @Operation(summary = "Удалить агент из реестра")
    public ResponseEntity<Void> unregisterAgent(@PathVariable String agentName) {
        toolRegistryService.unregister(agentName);
        return ResponseEntity.noContent().build();
    }
}