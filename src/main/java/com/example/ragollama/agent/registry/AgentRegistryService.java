package com.example.ragollama.agent.registry;

import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.registry.model.AgentDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Динамический реестр для агентов-инструментов.
 * <p>
 * Хранит в памяти карту зарегистрированных агентов и их определений,
 * предоставляя методы для их добавления, удаления и поиска.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRegistryService {

    private final AgentLoaderService agentLoaderService;
    private final ObjectMapper objectMapper;

    private final Map<String, QaAgent> agentMap = new ConcurrentHashMap<>();
    private final Map<String, AgentDefinition> definitionMap = new ConcurrentHashMap<>();

    /**
     * Регистрирует новый агент на основе его бина.
     *
     * @param agentBean Бин агента.
     */
    public void register(ToolAgent agentBean) {
        AgentDefinition definition = new AgentDefinition(
                agentBean.getName(),
                agentBean.getDescription(),
                agentBean.getClass().getName()
        );
        register(definition, agentBean);
    }

    /**
     * Регистрирует новый агент на основе его определения.
     *
     * @param definition DTO с описанием агента.
     */
    public void register(AgentDefinition definition) {
        ToolAgent agentInstance = agentLoaderService.loadAgent(definition);
        register(definition, agentInstance);
    }

    private void register(AgentDefinition definition, ToolAgent agentInstance) {
        agentMap.put(definition.name(), agentInstance);
        definitionMap.put(definition.name(), definition);
        log.info("Агент '{}' успешно зарегистрирован.", definition.name());
    }

    /**
     * Удаляет агент из реестра.
     *
     * @param agentName Имя агента.
     */
    public void unregister(String agentName) {
        agentMap.remove(agentName);
        definitionMap.remove(agentName);
        log.info("Агент '{}' успешно удален из реестра.", agentName);
    }

    /**
     * Находит агента по имени.
     *
     * @param agentName Имя агента.
     * @return {@link Optional} с агентом.
     */
    public Optional<QaAgent> getAgent(String agentName) {
        return Optional.ofNullable(agentMap.get(agentName));
    }

    /**
     * Возвращает список определений всех зарегистрированных агентов.
     *
     * @return
     */
    public Collection<AgentDefinition> getAllAgents() {
        return definitionMap.values();
    }

    /**
     * Форматирует описания всех зарегистрированных агентов в JSON.
     *
     * @return
     */
    public String getToolDescriptionsAsJson() {
        List<Map<String, String>> toolDescriptions = definitionMap.values().stream()
                .map(def -> Map.of("name", def.name(), "description", def.description()))
                .toList();
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(toolDescriptions);
        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать описания инструментов в JSON", e);
            return "[]";
        }
    }
}