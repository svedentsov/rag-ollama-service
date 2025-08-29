package com.example.ragollama.qaagent.dynamic;

import com.example.ragollama.qaagent.QaAgent;
import com.example.ragollama.qaagent.ToolAgent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис-реестр, который обнаруживает все доступные в приложении
 * **инструментальные** агенты и предоставляет информацию о них.
 */
@Slf4j
@Service
public class ToolRegistryService {

    private final ObjectProvider<List<ToolAgent>> allToolAgentsProvider;
    private final ObjectMapper objectMapper;
    private Map<String, QaAgent> agentMap;

    public ToolRegistryService(ObjectProvider<List<ToolAgent>> allToolAgentsProvider, ObjectMapper objectMapper) {
        this.allToolAgentsProvider = allToolAgentsProvider;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        List<ToolAgent> allToolAgents = allToolAgentsProvider.getObject();
        agentMap = allToolAgents.stream()
                .collect(Collectors.toMap(QaAgent::getName, Function.identity()));
        log.info("ToolRegistryService инициализирован. Зарегистрировано {} инструментов: {}", agentMap.size(), agentMap.keySet());
    }

    /**
     * Находит агента по его имени.
     *
     * @param agentName Имя агента.
     * @return {@link Optional} с агентом или пустой, если агент не найден.
     */
    public Optional<QaAgent> getAgent(String agentName) {
        return Optional.ofNullable(agentMap.get(agentName));
    }

    /**
     * Форматирует описания всех зарегистрированных агентов в JSON-строку.
     *
     * @return JSON-строка с "каталогом инструментов" для LLM-планировщика.
     */
    public String getToolDescriptionsAsJson() {
        List<Map<String, String>> toolDescriptions = agentMap.values().stream()
                .map(agent -> Map.of(
                        "name", agent.getName(),
                        "description", agent.getDescription()
                ))
                .toList();
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(toolDescriptions);
        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать описания инструментов в JSON", e);
            return "[]";
        }
    }
}
