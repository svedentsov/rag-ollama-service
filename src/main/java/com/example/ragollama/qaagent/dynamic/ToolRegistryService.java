package com.example.ragollama.qaagent.dynamic;

import com.example.ragollama.qaagent.QaAgent;
import com.example.ragollama.qaagent.ToolAgent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class ToolRegistryService {

    private final List<ToolAgent> allToolAgents;
    private final ObjectMapper objectMapper;
    private Map<String, QaAgent> agentMap;

    @PostConstruct
    public void init() {
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
        List<Map<String, String>> toolDescriptions = allToolAgents.stream()
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
