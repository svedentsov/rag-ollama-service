package com.example.ragollama.agent.dynamic;

import com.example.ragollama.agent.QaAgent;
import com.example.ragollama.agent.ToolAgent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис-реестр, который обнаруживает все доступные в приложении
 * **инструментальные** агенты и предоставляет информацию о них.
 * <p>
 * Этот сервис является "каталогом инструментов" для {@link PlanningAgentService}.
 * Он автоматически сканирует контекст приложения на наличие бинов, реализующих
 * маркерный интерфейс {@link ToolAgent}, и делает их доступными для
 * динамического планировщика.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolRegistryService {

    private final ObjectProvider<List<ToolAgent>> allToolAgentsProvider;
    private final ObjectMapper objectMapper;
    private Map<String, QaAgent> agentMap;

    /**
     * Инициализирует реестр после создания бина.
     * <p>
     * Метод использует {@link ObjectProvider} для безопасного получения списка
     * всех бинов {@link ToolAgent} и создает из них потокобезопасную карту
     * для быстрого доступа по имени.
     */
    @PostConstruct
    public void init() {
        List<ToolAgent> allToolAgents = allToolAgentsProvider.getIfAvailable(Collections::emptyList);
        this.agentMap = allToolAgents.stream()
                .collect(Collectors.toUnmodifiableMap(QaAgent::getName, Function.identity()));
        log.info("ToolRegistryService инициализирован. Зарегистрировано {} инструментов: {}", agentMap.size(), agentMap.keySet());
    }

    /**
     * Находит агента по его уникальному имени.
     *
     * @param agentName Имя агента (например, "git-inspector").
     * @return {@link Optional} с экземпляром агента, или пустой, если агент не найден.
     */
    public Optional<QaAgent> getAgent(String agentName) {
        return Optional.ofNullable(agentMap.get(agentName));
    }

    /**
     * Форматирует описания всех зарегистрированных агентов-инструментов в JSON-строку.
     * <p>
     * Эта строка передается в LLM-планировщик, чтобы он знал, какие
     * инструменты доступны для построения плана.
     *
     * @return JSON-строка, представляющая собой массив объектов с полями "name" и "description".
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
