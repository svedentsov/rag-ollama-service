package com.example.ragollama.agent.registry;

import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.registry.model.AgentDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Сервис-загрузчик, отвечающий за создание экземпляров агентов.
 * <p>
 * В этой реализации он использует {@link ApplicationContext} для получения
 * уже существующих бинов по имени класса, что является безопасным и
 * надежным способом.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentLoaderService {

    private final ApplicationContext applicationContext;

    /**
     * Загружает (создает) экземпляр агента на основе его определения.
     *
     * @param definition Описание агента.
     * @return Экземпляр {@link ToolAgent}.
     * @throws IllegalArgumentException если класс не найден или не является бином.
     */
    public ToolAgent loadAgent(AgentDefinition definition) {
        try {
            Class<?> agentClass = Class.forName(definition.implementationClass());
            Object bean = applicationContext.getBean(agentClass);
            if (bean instanceof ToolAgent toolAgent) {
                return toolAgent;
            } else {
                throw new IllegalArgumentException("Класс " + definition.implementationClass() + " не реализует интерфейс ToolAgent.");
            }
        } catch (ClassNotFoundException e) {
            log.error("Класс агента не найден: {}", definition.implementationClass(), e);
            throw new IllegalArgumentException("Класс не найден: " + definition.implementationClass(), e);
        }
    }
}