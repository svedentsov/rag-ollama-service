package com.example.ragollama.agent.copilot;

import com.example.ragollama.agent.AgentContext;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Представляет состояние одной диалоговой сессии с QA Copilot.
 * <p>
 * Этот объект хранится в кэше и содержит всю необходимую "память"
 * для ведения контекстуального диалога, включая историю сообщений
 * и результаты работы выполненных агентов.
 */
@Getter
public class CopilotSession implements Serializable {

    private final List<ChatMessage> history = new ArrayList<>();
    private final Map<String, Object> accumulatedContext = new HashMap<>();

    /**
     * "Полезная нагрузка" (payload) последнего результата работы агента.
     * Сохраняется для возможных последующих запросов на объяснение (XAI).
     * Поле помечено как transient, чтобы избежать проблем с сериализацией сложных объектов.
     */
    @Setter
    private transient Map<String, Object> lastAgentResult;

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
     *
     * @param role    Роль отправителя (USER или ASSISTANT).
     * @param content Текст сообщения.
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
