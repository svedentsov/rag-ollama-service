package com.example.ragollama.qaagent.copilot;

import com.example.ragollama.qaagent.AgentResult;
import com.example.ragollama.qaagent.api.dto.CopilotRequest;
import com.example.ragollama.qaagent.api.dto.CopilotResponse;
import com.example.ragollama.qaagent.dynamic.DynamicPipelineExecutionService;
import com.example.ragollama.qaagent.dynamic.PlanningAgentService;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Сервис-оркестратор для QA Copilot.
 * <p>
 * Является stateful-компонентом, который управляет диалоговыми сессиями,
 * вызывает stateless-планировщик и исполнитель, и суммирует результаты
 * для предоставления человекочитаемого ответа.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QaCopilotService {

    private final CopilotSessionService sessionService;
    private final PlanningAgentService planningAgentService;
    private final DynamicPipelineExecutionService executionService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * Обрабатывает сообщение от пользователя в рамках сессии.
     * <p>
     * Этот метод реализует полный асинхронный конвейер:
     * 1. Получает или создает сессию.
     * 2. Сохраняет сообщение пользователя в истории.
     * 3. Вызывает AI-планировщик, передавая ему задачу и весь накопленный контекст из сессии.
     * 4. Вызывает исполнитель для выполнения сгенерированного плана.
     * 5. Получает технические результаты от агентов.
     * 6. Вызывает LLM-суммаризатор для преобразования технических результатов в человекочитаемый Markdown.
     * 7. Обновляет сессию: добавляет ответ ассистента в историю и обогащает накопленный контекст результатами выполненных агентов.
     * 8. Возвращает финальный ответ и ID сессии пользователю.
     *
     * @param request DTO с запросом.
     * @return {@link Mono} с ответом копайлота.
     */
    public Mono<CopilotResponse> processUserMessage(CopilotRequest request) {
        UUID sessionId = sessionService.getOrCreateSessionId(request.sessionId());
        CopilotSession session = sessionService.getSession(sessionId);

        session.addMessage(new CopilotSession.ChatMessage(CopilotSession.Role.USER, request.message()));

        return planningAgentService.createPlan(request.message(), session.getAccumulatedContext())
                .flatMap(plan -> executionService.executePlan(plan, session.toAgentContext(), sessionId))
                .flatMap(results -> {
                    Mono<String> summaryMono = summarizeResults(request.message(), results);
                    return summaryMono.map(summary -> {
                        // Обновляем сессию, когда у нас есть и summary, и results
                        session.addMessage(new CopilotSession.ChatMessage(CopilotSession.Role.ASSISTANT, summary));
                        results.forEach(result -> session.updateContext(result.details()));
                        sessionService.updateSession(sessionId, session);
                        return summary; // Возвращаем summary дальше по цепочке
                    });
                })
                .map(summary -> new CopilotResponse(summary, sessionId));
    }

    /**
     * Преобразует "сырые" результаты работы агентов в человекочитаемый ответ.
     *
     * @param userQuery Запрос пользователя, инициировавший выполнение.
     * @param results   Список технических результатов от агентов.
     * @return {@link Mono} со строкой ответа в формате Markdown.
     */
    private Mono<String> summarizeResults(String userQuery, List<AgentResult> results) {
        if (results.isEmpty()) {
            return Mono.just("Я не смог выполнить эту задачу, так как не нашел подходящих инструментов или план выполнения был пуст.");
        }

        // Если последний результат требует утверждения, формируем специальный ответ
        AgentResult lastResult = results.get(results.size() - 1);
        if ("human-in-the-loop-gate".equals(lastResult.agentName())) {
            return Mono.just("Для продолжения выполнения задачи требуется ваше утверждение. " +
                    "ID операции для утверждения: `" + lastResult.details().get("executionId") + "`.\n\n" +
                    "Вы можете утвердить, выполнив запрос: `POST /api/v1/reviews/" + lastResult.details().get("executionId") + "/approve`");
        }

        try {
            String resultsAsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
            String promptString = promptService.render("copilotResultSummarizer", Map.of(
                    "userQuery", userQuery,
                    "resultsJson", resultsAsJson
            ));
            return Mono.fromFuture(llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED));
        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать результаты агентов в JSON", e);
            return Mono.just("Произошла внутренняя ошибка при форматировании ответа.");
        }
    }
}
