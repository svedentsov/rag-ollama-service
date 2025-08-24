package com.example.ragollama.qaagent.copilot;

import com.example.ragollama.qaagent.AgentContext;
import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Представляет состояние одной диалоговой сессии с QA Copilot.
 * <p>
 * Этот объект хранится в кэше и содержит всю необходимую "память"
 * для ведения контекстуального диалога.
 */
@Getter
public class CopilotSession implements Serializable {

    private final List<ChatMessage> history = new ArrayList<>();
    private final Map<String, Object> accumulatedContext = new HashMap<>();

    /**
     * Добавляет новое сообщение в историю диалога.
     *
     * @param message Сообщение для добавления.
     */
    public void addMessage(ChatMessage message) {
        this.history.add(message);
    }

    /**
     * Обновляет накопленный контекст, добавляя в него результаты
     * работы выполненных агентов.
     *
     * @param newDetails Карта с результатами последнего шага.
     */
    public void updateContext(Map<String, Object> newDetails) {
        this.accumulatedContext.putAll(newDetails);
    }

    /**
     * Создает AgentContext из накопленного состояния сессии.
     *
     * @return {@link AgentContext} для передачи в исполнитель.
     */
    public AgentContext toAgentContext() {
        return new AgentContext(this.accumulatedContext);
    }

    /**
     * Внутренний record для представления одного сообщения в сессии.
     */
    public record ChatMessage(Role role, String content) implements Serializable {
    }

    /**
     * Роли участников диалога.
     */
    public enum Role {
        USER, ASSISTANT
    }
}
