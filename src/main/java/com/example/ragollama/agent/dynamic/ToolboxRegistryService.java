package com.example.ragollama.agent.dynamic;

import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.Toolbox;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ToolboxRegistryService {

    private final ObjectProvider<List<ToolAgent>> allToolAgentsProvider;
    private Map<String, List<ToolAgent>> toolboxes;
    private Map<String, String> toolboxDescriptions;

    @PostConstruct
    public void init() {
        List<ToolAgent> agents = allToolAgentsProvider.getIfAvailable(Collections::emptyList);
        this.toolboxes = agents.stream()
                .filter(agent -> agent.getClass().isAnnotationPresent(Toolbox.class))
                .collect(Collectors.groupingBy(agent -> agent.getClass().getAnnotation(Toolbox.class).name()));

        this.toolboxDescriptions = agents.stream()
                .filter(agent -> agent.getClass().isAnnotationPresent(Toolbox.class))
                .map(agent -> agent.getClass().getAnnotation(Toolbox.class))
                .collect(Collectors.toMap(Toolbox::name, Toolbox::description, (existing, replacement) -> existing));
    }

    public List<ToolAgent> getToolbox(String name) {
        return toolboxes.getOrDefault(name, List.of());
    }

    public Map<String, String> getToolboxDescriptions() {
        return Collections.unmodifiableMap(toolboxDescriptions);
    }
}
