package com.example.ragollama.agent.registry;

import com.example.ragollama.agent.ToolAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Компонент, отвечающий за первоначальную регистрацию всех агентов,
 * которые являются Spring-бинами, при старте приложения.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AgentRegistryInitializer {

    private final ObjectProvider<List<ToolAgent>> toolAgentsProvider;
    private final ToolRegistryService registryService;

    /**
     * Выполняется после полного запуска приложения. Сканирует все бины
     * типа {@link ToolAgent} и регистрирует их в динамическом реестре.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeRegistry() {
        log.info("Запуск первоначальной регистрации агентов-инструментов...");
        List<ToolAgent> toolAgents = toolAgentsProvider.getIfAvailable(List::of);
        for (ToolAgent agent : toolAgents) {
            registryService.register(agent);
        }
        log.info("Первоначальная регистрация {} агентов завершена.", toolAgents.size());
    }
}